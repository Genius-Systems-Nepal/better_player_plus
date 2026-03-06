import Foundation
import AVFoundation

@objc public class BetterPlayerVuDrmAssetsLoaderDelegate: NSObject, AVAssetResourceLoaderDelegate {
    var certificateURL: String?
    var licenseURL: URL?
    var fairPlayToken: String?

    @objc public init(certificateURL: String? = nil, licenseURL: URL? = nil, fairPlayToken: String? = nil) {
        self.certificateURL = certificateURL
        self.licenseURL = licenseURL
        self.fairPlayToken = fairPlayToken
        super.init()
    }

    public func resourceLoader(
        _ resourceLoader: AVAssetResourceLoader,
        shouldWaitForLoadingOfRequestedResource loadingRequest: AVAssetResourceLoadingRequest
    ) -> Bool {
        guard let certUrlString = certificateURL, let certUrl = URL(string: certUrlString) else {
            loadingRequest.finishLoading()
            return false
        }

        var certRequest = URLRequest(url: certUrl)
        certRequest.httpMethod = "GET"
        certRequest.addValue(fairPlayToken ?? "", forHTTPHeaderField: "x-vudrm-token")

        URLSession.shared.dataTask(with: certRequest) { [weak self] certData, _, certError in
            guard let self = self else {
                loadingRequest.finishLoading()
                return
            }
            guard certError == nil, let certificateData = certData else {
                print("❌ Error on fetching certificate! -> \(certError?.localizedDescription ?? "unknown")")
                loadingRequest.finishLoading()
                return
            }
            guard let licenseUrl = loadingRequest.request.url else {
                print("❌ Error on extracting license url!")
                loadingRequest.finishLoading()
                return
            }
            print("✅ License url validation passed: -> \(licenseUrl)")

            let contentId = licenseUrl.lastPathComponent
            guard
                let contentIdData = contentId.data(using: .utf8),
                let spcData = try? loadingRequest.streamingContentKeyRequestData(
                    forApp: certificateData,
                    contentIdentifier: contentIdData,
                    options: nil
                ),
                let dataRequest = loadingRequest.dataRequest
            else {
                print("❌ Error on creating SPC Message!")
                loadingRequest.finishLoading()
                return
            }

            let targetLicenseUrl = self.licenseURL ?? URL(string: licenseUrl.absoluteString.replacingOccurrences(of: "skd", with: "https"))
            guard let postUrl = targetLicenseUrl else {
                loadingRequest.finishLoading()
                return
            }

            let payload: [String: Any?] = [
                "token": self.fairPlayToken,
                "contentId": contentId,
                "payload": spcData.base64EncodedString(),
            ]
            guard let body = try? JSONSerialization.data(withJSONObject: payload.compactMapValues { $0 }, options: []) else {
                loadingRequest.finishLoading()
                return
            }

            var ckcRequest = URLRequest(url: postUrl)
            ckcRequest.httpMethod = "POST"
            ckcRequest.httpBody = body
            ckcRequest.addValue("application/json", forHTTPHeaderField: "Content-Type")

            URLSession.shared.dataTask(with: ckcRequest) { ckcData, _, ckcError in
                if let ckcError = ckcError {
                    print("❌ Error on fetching CKC! -> \(ckcError.localizedDescription)")
                    loadingRequest.finishLoading()
                    return
                }
                if let ckcData = ckcData {
                    print("✅ CKC fetched successfully!")
                    dataRequest.respond(with: ckcData)
                } else {
                    print("❌ Error in CKC data!")
                }
                loadingRequest.finishLoading()
            }.resume()
        }.resume()

        return true
    }
}
