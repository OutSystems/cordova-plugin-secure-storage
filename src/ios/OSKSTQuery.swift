typealias OSKSTQueryDictionary = [String: Any]

final class OSKSTQuery: NSObject {
    enum OSKSTQueryItemQuantity {
        case one
        case all
    }
    
    private let service: String
    private let account: String?
    private let data: Data?
    
    init(service: String, account: String? = nil, data: Data? = nil) {
        self.service = service
        self.account = account
        self.data = data
    }
    
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
    
    func delete(quantity: OSKSTQueryItemQuantity) -> OSKSTError? {
        if quantity == .one {
            guard self.account != nil else { return .badArguments }
        }
        
        let query = self.commonQuery
        let status = SecItemDelete(query as CFDictionary)
        
        return status.queryError
    }
}

private extension OSKSTQuery {
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
    
    func getBioSecAccessControl() -> SecAccessControl? {
        SecAccessControlCreateWithFlags(nil, kSecAttrAccessibleWhenUnlocked, .userPresence, nil)
    }
}
