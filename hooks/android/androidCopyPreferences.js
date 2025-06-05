const path = require('path');
const fs = require('fs');
const { ConfigParser } = require('cordova-common');
const { DOMParser, XMLSerializer } = require('xmldom');

module.exports = function (context) {
    const projectRoot = context.opts.cordova.project ? context.opts.cordova.project.root : context.opts.projectRoot;
    const configXML = path.join(projectRoot, 'config.xml');
    const configParser = new ConfigParser(configXML);
    const parser = new DOMParser();

    const authenticate = configParser.getGlobalPreference('MigratedKeysAuthentication');
    const auth_prompt_title = configParser.getPreference('AuthPromptTitle', 'android')
    const auth_prompt_subtitle = configParser.getPreference('AuthPromptSubtitle', 'android')
    const auth_prompt_negative_button = configParser.getPreference('AuthPromptCancelButton', 'android')

    const stringsXmlPath = path.join(projectRoot, 'platforms/android/app/src/main/res/values/strings.xml');
    const stringsXmlString = fs.readFileSync(stringsXmlPath, 'utf-8');
    const stringsXmlDoc = parser.parseFromString(stringsXmlString, 'text/xml')

    // Keys to update and their values
    const boolKey = "migration_auth";
    const stringKeys = {
        biometric_prompt_title: auth_prompt_title,
        biometric_prompt_subtitle: auth_prompt_subtitle,
        biometric_prompt_negative_button: auth_prompt_negative_button
    };

    // === Remove duplicate <bool> entries ===
    const boolElements = Array.from(stringsXmlDoc.getElementsByTagName('bool'));
    const boolDuplicates = boolElements.filter(el => el.getAttribute('name') === boolKey);
    boolDuplicates.forEach(el => el.parentNode.removeChild(el));

    // Add new <bool> if needed
    if (authenticate == "true") {
        const newBool = stringsXmlDoc.createElement('bool');
        newBool.setAttribute('name', boolKey);
        newBool.textContent = authenticate;
        stringsXmlDoc.documentElement.appendChild(newBool);
    }

    // === Remove duplicate <string> entries ===
    const existingStrings = Array.from(stringsXmlDoc.getElementsByTagName('string'));

    for (const [key, value] of Object.entries(stringKeys)) {
        // Remove existing <string> with this name
        existingStrings
            .filter(el => el.getAttribute('name') === key)
            .forEach(el => el.parentNode.removeChild(el));

        // Add new <string> if value is not empty
        if (value && value.trim() !== "") {
            const newString = stringsXmlDoc.createElement('string');
            newString.setAttribute('name', key);
            newString.textContent = value;
            stringsXmlDoc.documentElement.appendChild(newString);
        }
    }

    // serialize the updated XML document back to string
    const serializer = new XMLSerializer();
    const updatedXmlString = serializer.serializeToString(stringsXmlDoc);

    // write the updated XML string back to the same file
    fs.writeFileSync(stringsXmlPath, updatedXmlString, 'utf-8');
};
