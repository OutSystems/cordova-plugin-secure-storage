import OSCore

@objc(SecureStorage)
class SecureStorage: CDVPlugin {
    var plugin: OSKSTActionDelegate?
    var callbackId: String = ""
    
    private var savedCommand: CDVInvokedUrlCommand?
    
    override func pluginInitialize() {
        self.plugin = OSKSTFactory.createKeystoreWrapper(withDelegate: self)
    }
    
    @objc func dataBecameAvailable(notification: Notification) {
        // Re-triggers the `init` method as before, using the stored command
        guard let command = self.savedCommand else { return }
        self.setup(command: command)
    }
    
    @objc(init:)
    func setup(command: CDVInvokedUrlCommand) {
        if #available(iOS 15, *) {
            // if Protected Data Acess is not yet available, the app observes the `dataBecomeAvailableNotification:`, so that the method resumes when the notification is triggered
            if !UIApplication.shared.isProtectedDataAvailable {
                self.savedCommand = command
                NotificationCenter.default.addObserver(self, selector: #selector(dataBecameAvailable(notification:)), name: UIApplication.protectedDataDidBecomeAvailableNotification, object: nil)
                return
            }
            
            // all good, we can remove what was added and proceed.
            self.savedCommand = nil;
            NotificationCenter.default.removeObserver(self, name: UIApplication.protectedDataDidBecomeAvailableNotification, object: nil)
        }
        
        self.callbackId = command.callbackId
        self.callbackSuccess()
    }
    
    @objc(get:)
    func get(command: CDVInvokedUrlCommand) {
        guard
            let service = command.argument(at: 0) as? String,
            let key = command.argument(at: 1) as? String
        else {
            self.callback(error: .badArguments)
            return
        }
        
        self.callbackId = command.callbackId
        self.commandDelegate.run { [weak self] in
            guard let self = self else { return }
            self.plugin?.read(service: service, account: key)
        }
    }
    
    @objc(set:)
    func set(command: CDVInvokedUrlCommand) {
        guard
            let service = command.argument(at: 0) as? String,
            let key = command.argument(at: 1) as? String,
            let value = command.argument(at: 2) as? String, let valueData = value.data(using: .utf8),
            let useAccessControl = command.argument(at: 3) as? Bool
        else {
            self.callback(error: .badArguments)
            return
        }
        
        self.callbackId = command.callbackId
        self.commandDelegate.run { [weak self] in
            guard let self = self else { return }
            self.plugin?.save(service: service, account: key, data: valueData, useAccessControl: useAccessControl)
        }
    }
    
    @objc(remove:)
    func remove(command: CDVInvokedUrlCommand) {
        guard
            let service = command.argument(at: 0) as? String,
            let key = command.argument(at: 1) as? String
        else {
            self.callback(error: .badArguments)
            return
        }
        
        self.callbackId = command.callbackId
        self.commandDelegate.run { [weak self] in
            guard let self = self else { return }
            self.plugin?.delete(service: service, account: key)
        }
    }
    
    @objc(keys:)
    func keys(command: CDVInvokedUrlCommand) {
        guard let service = command.argument(at: 0) as? String
        else {
            self.callback(error: .badArguments)
            return
        }
        
        self.callbackId = command.callbackId
        self.commandDelegate.run { [weak self] in
            guard let self = self else { return }
            self.plugin?.read(service: service)
        }
    }
    
    @objc(clear:)
    func clear(command: CDVInvokedUrlCommand) {
        guard let service = command.argument(at: 0) as? String
        else {
            self.callback(error: .badArguments)
            return
        }
        
        self.callbackId = command.callbackId
        self.commandDelegate.run { [weak self] in
            guard let self = self else { return }
            self.plugin?.delete(service: service)
        }
    }
}

// MARK: - OSCore's PlatformProtocol Methods
extension SecureStorage: PlatformProtocol {
    func sendResult(result: String?, error: NSError?, callBackID: String) {
        var pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR)

        if let error = error {
            let errorDict = [
                "code": "OS-PLUG-KSTR-\(String(format: "%04d", error.code))",
                "message": error.localizedDescription
            ]
            pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: errorDict);
        } else if let result = result {
            pluginResult = result.isEmpty ? CDVPluginResult(status: CDVCommandStatus_OK) : CDVPluginResult(status: CDVCommandStatus_OK, messageAs: result)
        }

        self.commandDelegate.send(pluginResult, callbackId: callBackID);
    }
}

// MARK: - SecureStorage's OSKSTCallbackDelegate Methods
extension SecureStorage: OSKSTCallbackDelegate {
    func callback(result: String?, error: OSKSTError?) {
        self.sendResult(result: result, error: error as? NSError, callBackID: self.callbackId)
    }
}
