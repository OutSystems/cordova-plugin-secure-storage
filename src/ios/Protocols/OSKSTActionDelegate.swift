/// Protocol that provides the server actions the plugin provides.
public protocol OSKSTActionDelegate: AnyObject {
    /// Allows the creation of a new Keychain item/update of an existing item.
    /// - Parameters:
    ///   - service: Represents the store associated with the item to be saved.
    ///   - account: Represents the key associated with the item to be saved.
    ///   - data: Represents the item's secret (e.g. password) to be saved.
    ///   - useAccessControl: Indicates if the item should be stored with an extra layer of security. It set to `true`, the item to be saved will request the device's local authentication (Face/Touch ID, Passcode) when requested.
    func save(service: String, account: String, data: Data, useAccessControl: Bool)
    
    /// Depending on the parameters passed, the method can retrieved two different things:
    /// - If the `account` parameter is passed, the stored `secret` associated with the passed `service` and `account` is retrieved.
    /// - If the `account` parameter is not passed, a list of keys associated with the passed `service` is retrieved.
    /// - Parameters:
    ///   - service: Represents the store associated with the stored item.
    ///   - account: Represents the key associated with the stored item. This parameters can be omitted.
    func read(service: String, account: String?)
    
    /// Depending on the parameters passed, the method can do two different things:
    /// - If the `account` parameter is passed, the item associated with the passed `service` and `account` is removed.
    /// - If the `account` parameter is not passed, the list of keys associated with the passed `service` is removed.
    /// - Parameters:
    ///   - service: Represents the store associated with the stored item.
    ///   - account: Represents the key associated with the stored item. This parameters can be omitted.
    func delete(service: String, account: String?)
}

// MARK: - Accelerators for the methods that can accept nil parameters
public extension OSKSTActionDelegate {
    /// Allows the retrieval of all the keys associated with the passed `service`.
    /// - Parameters:
    ///   - service: Represents the store associated with the stored item.
    func read(service: String) {
        self.read(service: service, account: nil)
    }
    
    /// Allows the removal of all the keys associated with the passed `service`.
    /// - Parameters:
    ///   - service: Represents the store associated with the stored item.
    func delete(service: String) {
        self.delete(service: service, account: nil)
    }
}
