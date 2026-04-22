// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "cordova-plugin-secure-storage",
    platforms: [.iOS(.v15)],
    products: [
        .library(
            name: "cordova-plugin-secure-storage",
            targets: ["SecureStoragePlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/apache/cordova-ios.git", branch: "master")
    ],
    targets: [
        .binaryTarget(
            name: "OSKeyStoreLib",
            path: "src/ios/frameworks/OSKeyStoreLib.xcframework"
        ),
        .target(
            name: "SecureStoragePlugin",
            dependencies: [
                .product(name: "Cordova", package: "cordova-ios"),
                .target(name: "OSKeyStoreLib")
            ],
            path: "src/ios",
            exclude: [
                "frameworks/OSKeyStoreLib.xcframework"
            ],
            sources: ["SecureStorage.swift"]
        )
    ]
)