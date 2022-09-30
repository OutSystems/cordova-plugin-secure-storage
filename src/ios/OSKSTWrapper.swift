/// The Keychain wrapper class. It provides a bridge between the library and its third party consumers.
class OSKSTWrapper: NSObject {
    /// Refers to the class that will handle all the return calls done by the  `OSKSTWrapper` class.
    private weak var delegate: OSKSTCallbackDelegate?
    
    /// Constructor method.
    /// - Parameter delegate: Handles the asynchronous return calls.
    init(delegate: OSKSTCallbackDelegate?) {
        self.delegate = delegate
    }
}

// MARK: - Action Methods to be called by the Bridge
extension OSKSTWrapper: OSKSTActionDelegate {
    /// Allows the creation of a new Keychain item/update of an existing item.
    /// - Parameters:
    ///   - service: Represents the store associated with the item to be saved.
    ///   - account: Represents the key associated with the item to be saved.
    ///   - data: Represents the item's secret (e.g. password) to be saved.
    ///   - useAccessControl: Indicates if the item should be stored with an extra layer of security. It set to `true`, the item to be saved will request the device's local authentication (Face/Touch ID, Passcode) when requested.
    func save(service: String, account: String, data: Data, useAccessControl: Bool) {
        let query = OSKSTQuery(service: service, account: account, data: data)
        if let error = query.save(useAccessControl) {
            self.delegate?.callback(error: error)
        } else {
            self.delegate?.callbackSuccess()
        }
    }
    
    /// Depending on the parameters passed, the method can retrieved two different things:
    /// - If the `account` parameter is passed, the stored `secret` associated with the passed `service` and `account` is retrieved.
    /// - If the `account` parameter is not passed, a list of keys associated with the passed `service` is retrieved.
    /// - Parameters:
    ///   - service: Represents the store associated with the stored item.
    ///   - account: Represents the key associated with the stored item. This parameters can be omitted.
    func read(service: String, account: String? = nil) {
        let query = OSKSTQuery(service: service, account: account)
        let result = query.fetch(quantity: account != nil ? .one : .all)
        switch result {
        case .failure(let error):
            self.delegate?.callback(error: error)
        case .success(let text):
            self.delegate?.callback(text: text)
        }
    }
    
    /// Depending on the parameters passed, the method can do two different things:
    /// - If the `account` parameter is passed, the item associated with the passed `service` and `account` is removed.
    /// - If the `account` parameter is not passed, the list of keys associated with the passed `service` is removed.
    /// - Parameters:
    ///   - service: Represents the store associated with the stored item.
    ///   - account: Represents the key associated with the stored item. This parameters can be omitted.
    func delete(service: String, account: String? = nil) {
        let query = OSKSTQuery(service: service, account: account)
        let error = query.delete(quantity: account != nil ? .one : .all)
        if let error = error {
            self.delegate?.callback(error: error)
        } else {
            self.delegate?.callback(text: account ?? "")
        }
    }
}
