const fs = require('fs');

const fixedBaseVersion = "2.6.8-OS";

module.exports = {
  branches: ['main'],
  tagFormat: '${version}',

  plugins: [
    {
      verifyConditions: () => {},
      analyzeCommits: () => {
        // since semantic-release expects an appropriate value for release
        //  but this plugin does not use semantic versioning, we just return 'patch' to allow the release checks to proceed
        return 'patch';
      },
      generateNotes: () => '',

      prepare: (pluginConfig, context) => {
        const pkg = JSON.parse(fs.readFileSync('./package.json', 'utf8'));
        const current = pkg.version;

        // Expected format: X.Y.Z-OS{A}  → split on "-OS"
        const base = fixedBaseVersion; // e.g. "1.2.3-OS"
        const basePrefix = base + "";

        let nextA = 1;

        if (current.startsWith(basePrefix)) {
          const suffix = current.replace(basePrefix, ''); // e.g. "12"
          const num = parseInt(suffix, 10);
          if (!isNaN(num)) nextA = num + 1;
        }

        const nextVersion = `${basePrefix}${nextA}`;

        context.nextRelease = {
          type: 'patch',
          version: nextVersion,
          gitTag: nextVersion
        };

        context.logger.log(`🔢 Next fixed version will be: ${nextVersion}`);
      },
    },
    '@semantic-release/commit-analyzer',
    '@semantic-release/release-notes-generator',
    [
      '@semantic-release/changelog',
      {
        changelogFile: 'CHANGELOG.md',
      },
    ],
    // updates to package version without npm publishing
    [
      '@semantic-release/npm',
      {
        pkgRoot: '.',
        npmPublish: false
      }
    ],
    // update plugin.xml version
    {
      async prepare(pluginConfig, context) {
        const { nextRelease } = context;
        const version = nextRelease.version;

        const xmlPath = 'plugin.xml';
        const xml = fs.readFileSync(xmlPath, 'utf8');

        // Detect current indentation from first indented line
        const match = xml.match(/^( +)\S/m);
        const indent = match ? match[1].length : 2; // fallback to 2 spaces if not found
        const parser = new xml2js.Parser();
        const builder = new xml2js.Builder({ renderOpts: { pretty: true, indent: ' '.repeat(indent) } });

        const parsed = await parser.parseStringPromise(xml);
        parsed.plugin.$.version = version;

        const updatedXml = builder.buildObject(parsed);
        fs.writeFileSync(xmlPath, updatedXml);

        console.log(`🔖 Updated plugin.xml version to ${version}`);
      }
    },
    [
      '@semantic-release/git',
      {
        assets: [
          'package.json',
          'plugin.xml',
          'CHANGELOG.md',
        ],
        message:
          'chore(release): ${nextRelease.version} [skip ci]\n\n${nextRelease.notes}',
      },
    ],
    [
      '@semantic-release/github',
      {
        successComment: false,
        failComment: false,
        releasedLabels: false,
        addReleases: 'bottom'
      }
    ],
  ]
};