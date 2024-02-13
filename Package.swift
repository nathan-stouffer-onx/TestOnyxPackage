// swift-tools-version: 5.9
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "TestOnyxPackage",
    products: [
        // Products define the executables and libraries a package produces, making them visible to other packages.
        .library(
            name: "OnyxCore",
            targets: [ "Assets", "bgfxPackage", "bimgPackage", "bimg_decodePackage", "bxPackage", "coreUtilsPackage", "cryptoPackage", "curlPackage", "onyxPackage", "nghttp2Package", "protobufPackage", "shaderPackage", /*"sqlite3Package",*/ "sslPackage", "stylingPackage", "testAppPackage", "thirdPartyPackage", "webpPackage", "webpdecoderPackage", "zPackage" ])
    ],
    targets: [
        // Targets are the basic building blocks of a package, defining a module or a test suite.
        // Targets can depend on other targets in this package and products from dependencies.
        .target(
            name: "Assets",
            resources: [
                .copy("assets")
            ]
        ),
        .binaryTarget(
            name: "bgfxPackage",
            path: "bgfx.xcframework.zip"
        ),
        .binaryTarget(
            name: "bimgPackage",
            path: "bimg.xcframework.zip"
        ),
        .binaryTarget(
            name: "bimg_decodePackage",
            path: "bimg_decode.xcframework.zip"
        ),
        .binaryTarget(
            name: "bxPackage",
            path: "bx.xcframework.zip"
        ),
        .binaryTarget(
            name: "cryptoPackage",
            path: "crypto.xcframework.zip"
        ),
        .binaryTarget(
            name: "curlPackage",
            path: "curl.xcframework.zip"
        ),
        .binaryTarget(
            name: "coreUtilsPackage",
            path: "coreUtils.xcframework.zip"
        ),
        .binaryTarget(
            name: "onyxPackage",
            path: "onyx.xcframework.zip"
        ),
        .binaryTarget(
            name: "nghttp2Package",
            path: "nghttp2.xcframework.zip"
        ),
        .binaryTarget(
            name: "protobufPackage",
            path: "protobuf.xcframework.zip"
        ),
        .binaryTarget(
            name: "shaderPackage",
            path: "shader.xcframework.zip"
        ),
        // TODO possibly re-enable this to get sqlite stuff working in our local apps (it works in iOS repo because they already have this)
        //.binaryTarget(
        //    name: "sqlite3Package",
        //    path: "sqlite3.xcframework.zip"
        //),
        .binaryTarget(
            name: "sslPackage",
            path: "ssl.xcframework.zip"
        ),
        .binaryTarget(
            name: "stylingPackage",
            path: "styling.xcframework.zip"
        ),
        .binaryTarget(
            name: "testAppPackage",
            path: "testApp.xcframework.zip"
        ),
        .binaryTarget(
            name: "thirdPartyPackage",
            path: "thirdParty.xcframework.zip"
        ),
        .binaryTarget(
            name: "webpPackage",
            path: "webp.xcframework.zip"
        ),
        .binaryTarget(
            name: "webpdecoderPackage",
            path: "webpdecoder.xcframework.zip"
        ),
        .binaryTarget(
            name: "zPackage",
            path: "z.xcframework.zip"
        )
    ],
    cxxLanguageStandard: .cxx17
)
