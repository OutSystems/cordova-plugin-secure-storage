const et = require('elementtree');
const path = require('path');
const fs = require('fs');
const { ConfigParser } = require('cordova-common');
const { DOMParser, XMLSerializer } = require('xmldom');

module.exports = function (context) {
    let projectRoot = context.opts.cordova.project ? context.opts.cordova.project.root : context.opts.projectRoot;
    let configXML = path.join(projectRoot, 'config.xml');
    let configParser = new ConfigParser(configXML);
    let authenticate = configParser.getGlobalPreference('MigratedKeysAuthentication');
    let auth_prompt_title = configParser.getPreference('AuthPromptTitle', 'android')
    let auth_prompt_subtitle = configParser.getPreference('AuthPromptSubtitle')
    let auth_prompt_negative_button = configParser.getPreference('AuthPromptCancelButton')

    const stringsXmlPath = path.join(projectRoot, 'platforms/android/app/src/main/res/values/strings.xml');
    const stringsXmlString = fs.readFileSync(stringsXmlPath, 'utf-8');
    const stringsXmlDoc = parser.parseFromString(stringsXmlString, 'text/xml')

    if(authenticate == "true"){
        /*
        var stringsXmlPath = path.join(projectRoot, 'platforms/android/app/src/main/res/values/strings.xml');
        var stringsXmlContents = fs.readFileSync(stringsXmlPath).toString();
        var etreeStrings = et.parse(stringsXmlContents);

        var migrationAuthTags = etreeStrings.findall('./bool[@name="migration_auth"]');
        for (var i = 0; i < migrationAuthTags.length; i++) {
            migrationAuthTags[i].text = authenticate;
        }

        var resultXmlStrings = etreeStrings.write();
        fs.writeFileSync(stringsXmlPath, resultXmlStrings);
        */

        // insert values in strings.xml
        

        const booleanElements = stringsXmlDoc.getElementsByTagName('bool');
        
        // set text for each <bool> element
        for (let i = 0; i < booleanElements.length; i++) {
            const name = booleanElements[i].getAttribute('name');
            if (name == "migration_auth") {
                booleanElements[i].textContent = authenticate;
            }
        }

    }

    const stringElements = stringsXmlDoc.getElementsByTagName('string');

    // set text for each <string> element
    for (let i = 0; i < stringElements.length; i++) {
        const name = stringElements[i].getAttribute('name');
        if (name == "biometric_prompt_title" && auth_prompt_title != "") {
            stringElements[i].textContent = auth_prompt_title;
        }
        else if (name == "biometric_prompt_subtitle" && auth_prompt_subtitle != "") {
            stringElements[i].textContent = auth_prompt_subtitle;
        }
        else if (name == "biometric_prompt_negative_button" && auth_prompt_negative_button) {
            stringElements[i].textContent = auth_prompt_negative_button;
        }
    }

    // serialize the updated XML document back to string
    const serializer = new XMLSerializer();
    const updatedXmlString = serializer.serializeToString(stringsXmlDoc);

    // write the updated XML string back to the same file
    fs.writeFileSync(stringsXmlPath, updatedXmlString, 'utf-8');


};
