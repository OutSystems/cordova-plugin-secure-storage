extension OSStatus {
    var queryError: OSKSTError? {
        switch self {
        case errSecSuccess:
            return nil
        case errSecUnimplemented:
            return .operationUnimplemented
        case errSecParam:
            return .invalidParameters
        case errSecAllocate:
            return .memoryAllocation
        case errSecNotAvailable:
            return .unavailable
        case errSecDuplicateItem:
            return .duplicateItem
        case errSecItemNotFound:
            return .itemNotFound
        case errSecInteractionNotAllowed:
            return .userInterationNotAllowed
        case errSecDecode:
            return .dataDecode
        case errSecAuthFailed:
            return .authenticationFailed
        default:
            return .general
        }
    }
}
