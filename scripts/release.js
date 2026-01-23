#!/usr/bin/env node

/**
 * Custom release script for Cordova Plugin with X.Y.Z-OS(A) versioning
 * 
 * This script:
 * 1. Increments the versionCode (A) in the version format X.Y.Z-OS(A)
 * 2. Updates plugin.xml and package.json with the new version
 * 3. Generates CHANGELOG entries from git commits since last release
 * 4. Creates a git tag and GitHub release
 */

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');
const xml2js = require('xml2js');
const simpleGit = require('simple-git');
const { Octokit } = require('@octokit/rest');

const BASE_VERSION = '2.6.8-OS';
const PLUGIN_XML_PATH = path.join(__dirname, '..', 'plugin.xml');
const PACKAGE_JSON_PATH = path.join(__dirname, '..', 'package.json');
const CHANGELOG_PATH = path.join(__dirname, '..', 'CHANGELOG');

// Initialize git and octokit
const git = simpleGit();
const octokit = new Octokit({
  auth: process.env.GITHUB_TOKEN,
});

/**
 * Parse version and extract versionCode
 * @param {string} version - Version string like "2.6.8-OS25"
 * @returns {number} - The versionCode (25)
 */
function getVersionCode(version) {
  if (!version.startsWith(BASE_VERSION)) {
    throw new Error(`Version ${version} does not match expected format ${BASE_VERSION}<number>`);
  }
  const code = parseInt(version.replace(BASE_VERSION, ''), 10);
  if (isNaN(code)) {
    throw new Error(`Could not parse versionCode from ${version}`);
  }
  return code;
}

/**
 * Generate next version string
 * @param {number} currentCode - Current versionCode
 * @returns {string} - Next version like "2.6.8-OS26"
 */
function getNextVersion(currentCode) {
  return `${BASE_VERSION}${currentCode + 1}`;
}

/**
 * Update version in plugin.xml
 * @param {string} newVersion - New version string
 */
async function updatePluginXml(newVersion) {
  const xml = fs.readFileSync(PLUGIN_XML_PATH, 'utf8');
  
  // Detect current indentation; use 2 spaces as fallback
  const match = xml.match(/^( +)\S/m);
  const indent = match ? match[1].length : 2;
  
  const parser = new xml2js.Parser();
  const builder = new xml2js.Builder({
    renderOpts: { pretty: true, indent: ' '.repeat(indent) },
    xmldec: { version: '1.0', encoding: 'UTF-8' }
  });
  
  const parsed = await parser.parseStringPromise(xml);
  parsed.plugin.$.version = newVersion;
  
  const updatedXml = builder.buildObject(parsed);
  fs.writeFileSync(PLUGIN_XML_PATH, updatedXml);
  
  console.log(`✅ Updated plugin.xml version to ${newVersion}`);
}

/**
 * Update version in package.json
 * @param {string} newVersion - New version string
 */
function updatePackageJson(newVersion) {
  const pkg = JSON.parse(fs.readFileSync(PACKAGE_JSON_PATH, 'utf8'));
  pkg.version = newVersion;
  fs.writeFileSync(PACKAGE_JSON_PATH, JSON.stringify(pkg, null, 2) + '\n');
  console.log(`✅ Updated package.json version to ${newVersion}`);
}

/**
 * Get commits since last tag
 * @returns {Array<{hash: string, message: string, date: string}>}
 */
async function getCommitsSinceLastTag() {
  try {
    // Get all local tags (not from remotes)
    // Use git command directly to ensure we only get local tags
    let tagList = [];
    try {
      const tagOutput = await git.raw(['tag', '-l']);
      tagList = tagOutput.trim().split('\n').filter(tag => tag.length > 0);
    } catch (error) {
      console.warn('⚠️  Could not list tags:', error.message);
    }
    
    // Filter tags to only those matching our version format (X.Y.Z-OS(A))
    // Example: 2.6.8-OS25, 2.6.8-OS24, etc.
    const versionTagPattern = new RegExp(`^${BASE_VERSION.replace('.', '\\.')}\\d+$`);
    const versionTags = tagList.filter(tag => versionTagPattern.test(tag));
    
    if (versionTags.length === 0) {
      // No version tags found, get all commits (excluding merge commits)
      console.log('📋 No version tags found, getting all commits...');
      const log = await git.log(['--no-merges']);
      const commits = log.all.map(commit => ({
        hash: commit.hash.substring(0, 7),
        message: commit.message,
        date: commit.date,
      }));
      
      if (commits.length === 0) {
        throw new Error('No commits found in repository');
      }
      
      return commits;
    }
    
    // Sort tags by versionCode (the number after -OS)
    versionTags.sort((a, b) => {
      const codeA = parseInt(a.replace(BASE_VERSION, ''), 10);
      const codeB = parseInt(b.replace(BASE_VERSION, ''), 10);
      return codeA - codeB;
    });
    
    // Get the latest tag (highest versionCode)
    const latestTag = versionTags[versionTags.length - 1];
    console.log(`📋 Found latest tag: ${latestTag}`);
    
    // Verify the tag exists in the current repository
    let tagCommit;
    try {
      tagCommit = await git.revparse([latestTag]);
    } catch (error) {
      throw new Error(`Tag ${latestTag} does not exist in this repository. It may be from an upstream remote.`);
    }
    
    const headCommit = await git.revparse(['HEAD']);
    
    if (tagCommit === headCommit) {
      // HEAD is at the same commit as the latest tag, no new commits
      return [];
    }
    
    // Get commits between latest tag and HEAD (excluding merge commits and the tag commit itself)
    // Use raw git command to exclude merge commits
    const log = await git.log([`${latestTag}..HEAD`, '--no-merges']);
    
    // Filter out the tag commit itself if it's included
    const commits = log.all
      .filter(commit => commit.hash !== tagCommit)
      .map(commit => ({
        hash: commit.hash.substring(0, 7),
        message: commit.message,
        date: commit.date,
      }));
    
    return commits;
  } catch (error) {
    // If it's our custom error about no commits, re-throw it
    if (error.message === 'No commits found in repository' || error.message.includes('No new commits')) {
      throw error;
    }
    console.warn('⚠️  Could not get commits since last tag:', error.message);
    throw new Error(`Failed to get commits: ${error.message}`);
  }
}

/**
 * Parse conventional commit message
 * @param {string} message - Raw commit message
 * @returns {{type: string, scope: string|null, message: string}} - Parsed commit
 */
function parseCommitMessage(message) {
  // Split by newline and take first line
  let formatted = message.trim().split('\n')[0];
  
  // Try to parse conventional commit format: type(scope): message
  // Examples: "fix(android): message", "feat: message", "chore(ios): message"
  const conventionalMatch = formatted.match(/^(\w+)(?:\(([^)]+)\))?:\s*(.+)$/i);
  
  if (conventionalMatch) {
    return {
      type: conventionalMatch[1].toLowerCase(),
      scope: conventionalMatch[2] || null,
      message: conventionalMatch[3].trim()
    };
  }
  
  // If not conventional format, try to infer type from common prefixes
  const lowerMsg = formatted.toLowerCase();
  let type = 'chore';
  let scope = null;
  let msg = formatted;
  
  if (lowerMsg.startsWith('feat:') || lowerMsg.startsWith('feature:')) {
    type = 'feat';
    msg = formatted.replace(/^feat(ure)?:\s*/i, '');
  } else if (lowerMsg.startsWith('fix:') || lowerMsg.startsWith('fixes:')) {
    type = 'fix';
    msg = formatted.replace(/^fix(es)?:\s*/i, '');
  } else if (lowerMsg.startsWith('chore:') || lowerMsg.startsWith('chores:')) {
    type = 'chore';
    msg = formatted.replace(/^chore(s)?:\s*/i, '');
  }
  
  // Try to extract scope from message if it contains platform indicators
  const scopeMatch = msg.match(/\[(android|ios|windows|browser)\]/i);
  if (scopeMatch) {
    scope = scopeMatch[1].toLowerCase();
    msg = msg.replace(/\[(android|ios|windows|browser)\]\s*/i, '').trim();
  }
  
  return {
    type,
    scope,
    message: msg || formatted
  };
}

/**
 * Format commit for changelog entry
 * @param {{type: string, scope: string|null, message: string}} parsed - Parsed commit
 * @returns {string} - Formatted changelog line
 */
function formatCommitForChangelog(parsed) {
  const scopePart = parsed.scope ? `(${parsed.scope})` : '';
  return `- ${parsed.type}${scopePart}: ${parsed.message}`;
}

/**
 * Generate changelog entry
 * @param {string} version - New version
 * @param {Array} commits - Array of commit objects
 * @returns {string} - Changelog entry
 */
function generateChangelogEntry(version, commits) {
  const today = new Date().toISOString().split('T')[0];
  let entry = `## [${version}]\n\n`;
  entry += `## ${today}\n\n`;
  
  if (commits.length === 0) {
    entry += '- No changes\n\n';
    return entry;
  }
  
  // Parse and group commits by type
  const features = [];
  const fixes = [];
  const chores = [];
  const others = [];
  
  commits.forEach(commit => {
    const parsed = parseCommitMessage(commit.message);
    const formatted = formatCommitForChangelog(parsed);
    
    if (parsed.type === 'feat') {
      features.push(formatted);
    } else if (parsed.type === 'fix') {
      fixes.push(formatted);
    } else if (parsed.type === 'chore') {
      chores.push(formatted);
    } else {
      others.push(formatted);
    }
  });
  
  // Output in order: Features, Fixes, Chores, Others
  if (features.length > 0) {
    features.forEach(f => entry += `${f}\n`);
  }
  if (fixes.length > 0) {
    fixes.forEach(f => entry += `${f}\n`);
  }
  if (chores.length > 0) {
    chores.forEach(f => entry += `${f}\n`);
  }
  if (others.length > 0) {
    others.forEach(f => entry += `${f}\n`);
  }
  
  entry += '\n';
  return entry;
}

/**
 * Update CHANGELOG file
 * @param {string} entry - New changelog entry
 */
function updateChangelog(entry) {
  let changelog = fs.readFileSync(CHANGELOG_PATH, 'utf8');
  
  // Check if file starts with "# Changelog" header (markdown format)
  const markdownHeaderMatch = changelog.match(/^# Changelog\n\n/i);
  if (markdownHeaderMatch) {
    // Insert after markdown header
    changelog = changelog.replace(markdownHeaderMatch[0], markdownHeaderMatch[0] + entry);
  } else {
    // If no header, prepend the entry
    changelog = entry + (changelog ? '\n' + changelog : '');
  }
  
  fs.writeFileSync(CHANGELOG_PATH, changelog);
  console.log(`✅ Updated CHANGELOG`);
}

/**
 * Get repository owner and name from git remote
 * @returns {{owner: string, repo: string}}
 */
async function getRepoInfo() {
  const remotes = await git.getRemotes(true);
  const origin = remotes.find(r => r.name === 'origin');
  
  if (!origin) {
    throw new Error('Could not find origin remote');
  }
  
  // Parse URL like https://github.com/owner/repo.git or git@github.com:owner/repo.git
  const url = origin.refs.fetch || origin.refs.push;
  const match = url.match(/github\.com[:/]([^/]+)\/([^/]+?)(?:\.git)?$/);
  
  if (!match) {
    throw new Error(`Could not parse repository from URL: ${url}`);
  }
  
  return {
    owner: match[1],
    repo: match[2],
  };
}

/**
 * Create GitHub release
 * @param {string} version - Version tag
 * @param {string} changelogEntry - Changelog entry for release notes
 */
async function createGitHubRelease(version, changelogEntry) {
  if (!process.env.GITHUB_TOKEN) {
    console.warn('⚠️  GITHUB_TOKEN not set, skipping GitHub release creation');
    return;
  }
  
  try {
    const { owner, repo } = await getRepoInfo();
    
    // Clean up changelog entry for release notes (remove markdown headers)
    const releaseNotes = changelogEntry
      .replace(/^## \[.*?\]\n\n## \d{4}-\d{2}-\d{2}\n\n/, '')
      .trim();
    
    await octokit.repos.createRelease({
      owner,
      repo,
      tag_name: version,
      name: version,
      body: releaseNotes || `Release ${version}`,
    });
    
    console.log(`✅ Created GitHub release: ${version}`);
  } catch (error) {
    console.error('❌ Failed to create GitHub release:', error.message);
    throw error;
  }
}

/**
 * Main release function
 */
async function main() {
  try {
    console.log('🚀 Starting release process...\n');
    
    // 1. Read current version
    const pkg = JSON.parse(fs.readFileSync(PACKAGE_JSON_PATH, 'utf8'));
    const currentVersion = pkg.version;
    console.log(`📦 Current version: ${currentVersion}`);
    
    // 2. Calculate next version
    const currentCode = getVersionCode(currentVersion);
    const nextVersion = getNextVersion(currentCode);
    console.log(`🔢 Next version: ${nextVersion}\n`);
    
    // 3. Get commits since last tag
    console.log('📋 Gathering commits since last release...');
    const commits = await getCommitsSinceLastTag();
    console.log(`   Found ${commits.length} commit(s)\n`);
    
    // Check if there are no new commits
    if (commits.length === 0) {
      throw new Error('No new commits since last release. Nothing to release.');
    }
    
    // 4. Generate changelog entry
    const changelogEntry = generateChangelogEntry(nextVersion, commits);
    console.log('📝 Generated changelog entry:');
    console.log(changelogEntry);
    
    // 5. Update files
    console.log('📝 Updating files...');
    await updatePluginXml(nextVersion);
    updatePackageJson(nextVersion);
    updateChangelog(changelogEntry);
    
    // 6. Stage changes
    console.log('\n📤 Staging changes...');
    await git.add([PLUGIN_XML_PATH, PACKAGE_JSON_PATH, CHANGELOG_PATH]);
    
    // 7. Commit changes
    console.log('💾 Committing changes...');
    await git.commit(`chore(release): ${nextVersion} [skip ci]\n\n${changelogEntry}`);
    
    // 8. Create tag
    console.log('🏷️  Creating git tag...');
    await git.addTag(nextVersion);
    
    // 9. Push to remote
    console.log('📤 Pushing to remote...');
    const currentBranch = await git.revparse(['--abbrev-ref', 'HEAD']);
    await git.push('origin', currentBranch);
    await git.pushTags('origin');
    
    // 10. Create GitHub release
    console.log('\n🎉 Creating GitHub release...');
    await createGitHubRelease(nextVersion, changelogEntry);
    
    console.log('\n✅ Release completed successfully!');
    console.log(`   Version: ${nextVersion}`);
    console.log(`   Tag: ${nextVersion}`);
    
  } catch (error) {
    console.error('\n❌ Release failed:', error.message);
    process.exit(1);
  }
}

// Run if called directly
if (require.main === module) {
  main();
}

module.exports = { main };
