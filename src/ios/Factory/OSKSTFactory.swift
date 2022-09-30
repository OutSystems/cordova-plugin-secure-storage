/// Factory structure that creates a Keychain Wrapper
public struct OSKSTFactory {
    /// Method that creates a Keychain Wrapper of type `OSKSTWrapper`.
    /// - Parameter delegate: Object responsible for the callback calls of the `OSKSTWrapper` class.
    /// - Returns: An instance of the `OSKSTWrapper` class.
    static func createKeystoreWrapper(withDelegate delegate: OSKSTCallbackDelegate) -> OSKSTActionDelegate {
        return OSKSTWrapper(delegate: delegate)
    }
}
