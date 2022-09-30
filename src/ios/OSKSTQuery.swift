typealias OSKSTQueryDictionary = [String: Any]

/// Interface used for querying or modifying Keychain items.
final class OSKSTQuery: NSObject {
    /// Indicates if the query should address a single or all the retrieved items.
    enum OSKSTQueryItemQuantity {
        case one
        case all
    }
    
    /// Represents the Keychain item's store property. It corresponds to `kSecAttrAccount`.
    private let service: String
    
    /// Represents the Keychain item's key property. It corresponds to `kSecAttrService`.
    private let account: String?
    
    /// Represents the Keychain item's secret property. It corresponds to `kSecValueData`.
    private let data: Data?
    
    /// Constructor method.
    /// - Parameters:
    ///   - service: Represents the Keychain item's store property.
    ///   - account: Represents the Keychain item's key property. It can be defined as nil.
    ///   - data: Represents the Keychain item's secret property. It can be defined as nil.
    init(service: String, account: String? = nil, data: Data? = nil) {
        self.service = service
        self.account = account
        self.data = data
    }
    
    /// Saves the receiver's attributes as a Keychain item. It creates a new item or updates an existing one depending if there are any associated items to the passed `service` and `account` properties.
    /// - Parameter useAccessControl: Indicates if an extra layer of security should be included on item retrieval. This layer relates to a device's local authentication (Face/Touch ID and Passcode).
    /// - Returns: The method returns an error in case of failure of the operation. Returns `nil` in case of success.
    func save(_ useAccessControl: Bool) -> OSKSTError? {
        guard self.account != nil, let passwordData = self.data else { return .badArguments }
        
        let searchQuery = self.commonQuery
        var result: AnyObject?
        var status = SecItemCopyMatching(searchQuery as CFDictionary, &result)
        
        guard status == errSecSuccess || status == errSecItemNotFound else {
            return status.queryError
        }

        var query: OSKSTQueryDictionary = [kSecValueData as String: passwordData]
        if useAccessControl {
            if let accessControl = self.getBioSecAccessControl() {
                query[kSecAttrAccessControl as String] = accessControl
            }
        } else {
            query[kSecAttrAccessible as String] = kSecAttrAccessibleWhenUnlocked
        }
        
        if status == errSecSuccess {
            status = SecItemUpdate(searchQuery as CFDictionary, query as CFDictionary)
        } else {
            query = query.merging(searchQuery) { $1 }
            status = SecItemAdd(query as CFDictionary, nil)
        }
        
        return status.queryError
    }
    
    /// Fetches the keychain items that matches the given `service` and `account` (if passed).
    /// - Parameter quantity: Indicates if it should be returned a single or multiple matching items.
    /// - Returns: The method can return three different results. An error, if the fetching operation fails; the item's `secret` in case of the parameter `OSKSTQueryItemQuantity.one` or all the matching keys in case of `OSKSTQueryItemQuantity.all`.
    func fetch(quantity: OSKSTQueryItemQuantity) -> Result<String, OSKSTError> {
        if quantity == .one {
            guard self.account != nil else { return .failure(.badArguments) }
        }
        
        let query = self.readQuery(withLimit: quantity)
        var queryResult: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &queryResult)
        
        guard status == errSecSuccess else {
            if quantity == .all && status == errSecItemNotFound {
                return .success("")
            }
            return .failure(status.queryError ?? .general)
        }
        
        if quantity == .one {
            guard let data = queryResult as? Data, let result = String(data: data, encoding: .utf8)
            else { return .failure(.general) }
            
            return .success(result)
        } else {
            let array = queryResult as! CFArray // swiftlint:disable:this force_cast
            let queryDictArray: [OSKSTQueryDictionary] = array.toSwiftArray()
            let result = queryDictArray.compactMap { $0[kSecAttrAccount as String] as? String }
            
            return .success(result.joined(separator: ","))
        }
    }
    
    /// Deletes the Keychain items that matches the given `service` and `account` (if passed).
    /// - Parameter quantity: Indicates if it should be removed a single or multiple matching items.
    /// - Returns: The method returns an error in case of failure of the operation. Returns `nil` in case of success.
    func delete(quantity: OSKSTQueryItemQuantity) -> OSKSTError? {
        if quantity == .one {
            guard self.account != nil else { return .badArguments }
        }
        
        let query = self.commonQuery
        let status = SecItemDelete(query as CFDictionary)
        
        return status.queryError
    }
}

// MARK: - Private methods used by the Class
private extension OSKSTQuery {
    /// Query parameters that are used by all method calls.
    var commonQuery: OSKSTQueryDictionary {
        var query: OSKSTQueryDictionary = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: self.service
        ]
        if let account = self.account {
            query[kSecAttrAccount as String] = account
        }
        
        return query
    }
    
    /// In case of a `fetch` method, this method adds the necessary properties to the query, based on the `limit` parameter.
    /// - Parameter limit: Indicates if the query should be prepared to retrieve a single or multiple items.
    /// - Returns: The corresponding  `fetch` query.
    func readQuery(withLimit limit: OSKSTQueryItemQuantity) -> OSKSTQueryDictionary {
        var query = self.commonQuery
        if limit == .one {
            query[kSecReturnData as String] = true
            query[kSecMatchLimit as String] = kSecMatchLimitOne
        } else {
            query[kSecReturnAttributes as String] = true
            query[kSecMatchLimit as String] = kSecMatchLimitAll
        }
        return query
    }
    
    /// Creates a new access control object that offers an extra layer of protection to the Keychain item that corresponds to the device's local authentication.
    /// - Returns: If successful, it returns the Access Control object to include in the `save` query. Returns `nil`, otherwise.
    func getBioSecAccessControl() -> SecAccessControl? {
        SecAccessControlCreateWithFlags(nil, kSecAttrAccessibleWhenUnlocked, .userPresence, nil)
    }
}
