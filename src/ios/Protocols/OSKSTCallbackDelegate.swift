/// Delegate for the callback return calls for the plugin
public protocol OSKSTCallbackDelegate: AnyObject {
    func callback(result: String?, error: OSKSTError?)
}

// MARK: OSKSTCallbackDelegate Default Implementation
extension OSKSTCallbackDelegate {
    /// Triggers the callback when there's an error
    /// - Parameter error: Error to be thrown
    func callback(error: OSKSTError) {
        self.callback(result: nil, error: error)
    }
    
    /// Triggers the callback when there's a success text
    /// - Parameter text: Text to be returned
    func callback(text: String) {
        self.callback(result: text, error: nil)
    }
    
    /// Triggers the callback when there's a success without text
    func callbackSuccess() {
        self.callback(text: "")
    }
}
