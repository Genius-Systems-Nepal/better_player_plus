import AVFoundation
import Foundation
import QuanteecCore
import QuanteecPluginAVPlayer

enum QuanteecHelper {
    /// Mirrors [QUANTEEC AVPlayer setup](https://doc.quanteec.com/native-players/iOS/AVPlayer/): configure, set `videoID`, then `QuanteecPlugin(player:)`.
    static func setup(player: AVPlayer, quanteecConfig: [String: Any]) -> AnyObject? {
        let quanteecKey = (quanteecConfig["qunateecKey"] as? String) ?? (quanteecConfig["quanteecKey"] as? String) ?? ""
        guard !quanteecKey.isEmpty else {
            return nil
        }

        QuanteecConfig.configure(quanteecKey: quanteecKey)
        if let videoId = quanteecConfig["videoId"] as? String, !videoId.isEmpty {
            QuanteecConfig.shared.videoID = videoId
        }
        
//        QLoggerConfig.shared.showAnalytics = true
//        QLoggerConfig.shared.activeLogLevel = .trace

        return QuanteecPlugin(player: player)
    }
}
