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

        // create XML with correct values directly
    var stringsXmlPath = path.join(projectRoot, 'platforms/android/app/src/main/res/values/os_sociallogins_strings.xml');

    const xmlContent = `<?xml version='1.0' encoding='utf-8'?>
<resources>
    <bool name="migration_auth">false</bool>
    <string name="biometric_prompt_title">Authentication required</string>
    <string name="biometric_prompt_subtitle">Please authenticate to continue</string>
    <string name="biometric_prompt_negative_button">Cancel</string>
</resources>`;

    // write XML file directly
    fs.writeFileSync(stringsXmlPath, xmlContent);

    const stringsXmlString = fs.readFileSync(stringsXmlPath, 'utf-8');
    const stringsXmlDoc = parser.parseFromString(stringsXmlString, 'text/xml')

    // Keys to update and their values
    const boolKey = "migration_auth";
    const stringKeys = {
        biometric_prompt_title: auth_prompt_title,
        biometric_prompt_subtitle: auth_prompt_subtitle,
        biometric_prompt_negative_button: auth_prompt_negative_button
    };

    // process <bool> entry
    const boolElements = Array.from(stringsXmlDoc.getElementsByTagName('bool'));
    const boolMatches = boolElements.filter(el => el.getAttribute('name') === boolKey);

    if (authenticate == "true") {
        if (boolMatches.length > 0) {
            // remove any duplicates beyond the first
            for (let i = 1; i < boolMatches.length; i++) {
                boolMatches[i].parentNode.removeChild(boolMatches[i]);
            }

            // update first match if needed
            const existingBool = boolMatches[0];
            if (existingBool.textContent !== authenticate) {
                existingBool.textContent = authenticate;
            }
        } else {
            // add new <bool> if it doesn't exist
            const newBool = stringsXmlDoc.createElement('bool');
            newBool.setAttribute('name', boolKey);
            newBool.textContent = authenticate;
            stringsXmlDoc.documentElement.appendChild(newBool);
        }
    }

    // process <string> entries
    const allStrings = Array.from(stringsXmlDoc.getElementsByTagName('string'));

    for (const [key, value] of Object.entries(stringKeys)) {
        if (!value || value.trim() === "") continue;

        const matchingStrings = allStrings.filter(el => el.getAttribute('name') === key);

        if (matchingStrings.length > 0) {
            // remove duplicates beyond the first
            for (let i = 1; i < matchingStrings.length; i++) {
                matchingStrings[i].parentNode.removeChild(matchingStrings[i]);
            }

            // update first if needed
            const existingString = matchingStrings[0];
            if (existingString.textContent !== value) {
                existingString.textContent = value;
            }
        } else {
            // add new <string> if it doesn't exist
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
