import Foundation
import AVKit

@objc public class NerdStatHelper: NSObject {
    @objc public func getNerdStatText(player: AVPlayer?) -> String {
        guard let currentItem = player?.currentItem,
              let accessLogEvent = currentItem.accessLog()?.events.last else {
            return ""
        }

        let nerdsStatus = NerdsStats()
        if nerdsStatus.videoResolution == nil {
            nerdsStatus.videoResolution = currentItem.presentationSize
        }
        if nerdsStatus.videoFormat == nil {
            if let assetTrack = currentItem.tracks.first?.assetTrack, assetTrack.mediaType == .video {
                nerdsStatus.videoFormat = assetTrack.mediaFormat
            }
            if let assetTrack = currentItem.tracks.last?.assetTrack, assetTrack.mediaType == .audio {
                nerdsStatus.audioFormat = assetTrack.mediaFormat
            }
        }

        nerdsStatus.droppedFrames = accessLogEvent.numberOfDroppedVideoFrames
        nerdsStatus.bandwidthInBit = accessLogEvent.observedBitrate
        nerdsStatus.numberOfBytesTransferred = accessLogEvent.numberOfBytesTransferred
        nerdsStatus.bufferHealth = currentItem.bufferHealth()
        return getText(detail: nerdsStatus)
    }

    private func getText(detail: NerdsStats) -> String {
        let noDataText = "---"
        var str = ""

        let bufferHealthTitle = "Buffer Health:"
        if let bufferHealth = detail.bufferHealth {
            let value = bufferHealth.doubleValue.stringAfterLimitingPrecisions() ?? noDataText
            str = "\(bufferHealthTitle) \(value) s"
        } else {
            str = "\(bufferHealthTitle) \(noDataText)"
        }

        let bandwidthTitle = "Bandwidth:"
        if let bandwidthBit = detail.bandwidthInBit {
            if bandwidthBit < 1048576 {
                if let value = (bandwidthBit / 1024).stringAfterLimitingPrecisions() {
                    str += "\n\(bandwidthTitle) \(value) kbps"
                }
            } else if let value = (bandwidthBit / 1048576).stringAfterLimitingPrecisions() {
                str += "\n\(bandwidthTitle) \(value) mbps"
            }
        } else {
            str += "\n\(bandwidthTitle) \(noDataText)"
        }

        let videoFormatTitle = "Video:"
        if let videoFormat = detail.videoFormat {
            var text = videoFormat
            if let resolution = detail.videoResolution {
                text += " (\(Int(resolution.width)) x \(Int(resolution.height)))"
            }
            str += "\n\(videoFormatTitle) \(text)"
        } else {
            str += "\n\(videoFormatTitle) \(noDataText)"
        }

        str += "\nAudio: \(detail.audioFormat ?? noDataText)"

        let networkActivityTitle = "Network Activity:"
        if let bytesTransferred = detail.numberOfBytesTransferred {
            str += "\n\(networkActivityTitle) \(bytesTransferred / 1048576) MB"
        } else {
            str += "\n\(networkActivityTitle) \(noDataText)"
        }

        let framesDroppedTitle = "Framedrop:"
        if let totalFramesDropped = detail.droppedFrames {
            str += "\n\(framesDroppedTitle) \(totalFramesDropped)"
        } else {
            str += "\n\(framesDroppedTitle) \(noDataText)"
        }
        return str
    }
}

class NerdsStats: NSObject {
    var videoFormat: String?
    var audioFormat: String?
    var videoResolution: CGSize?

    var droppedFrames: Int?
    var bandwidthInBit: Double?
    var numberOfBytesTransferred: Int64?
    var bufferHealth: NSNumber?
}

extension AVAssetTrack {
    var mediaFormat: String {
        var format = ""
        let descriptions = self.formatDescriptions as! [CMFormatDescription]
        for (index, formatDesc) in descriptions.enumerated() {
            let type = CMFormatDescriptionGetMediaType(formatDesc).toString()
            let subType = CMFormatDescriptionGetMediaSubType(formatDesc).toString()
            format += "\(type)/\(subType)"
            if index < descriptions.count - 1 {
                format += ","
            }
        }
        return format
    }
}

extension AVPlayerItem {
    func bufferHealth() -> NSNumber? {
        let timeRanges: [NSValue] = self.loadedTimeRanges
        if timeRanges.isEmpty {
            return nil
        }
        let currentTime = self.currentTime()
        guard let timeRange = getTimeRange(timeRanges: timeRanges, forCurrentTime: currentTime) else {
            return nil
        }
        return max(timeRange.end.seconds - timeRange.start.seconds, 0) as NSNumber
    }

    func getTimeRange(timeRanges: [NSValue], forCurrentTime time: CMTime) -> CMTimeRange? {
        let timeRange = timeRanges.first(where: { value in
            CMTimeRangeContainsTime(value.timeRangeValue, time: time)
        })
        if timeRange == nil && !timeRanges.isEmpty {
            return timeRanges.first!.timeRangeValue
        }
        return timeRange?.timeRangeValue
    }
}

extension FourCharCode {
    func toString() -> String {
        let bytes: [CChar] = [
            CChar((self >> 24) & 0xff),
            CChar((self >> 16) & 0xff),
            CChar((self >> 8) & 0xff),
            CChar(self & 0xff),
            0,
        ]
        let result = String(cString: bytes)
        return result.trimmingCharacters(in: CharacterSet.whitespaces)
    }
}

extension Double {
    func stringAfterLimitingPrecisions(minFractionDigits: Int = 0, maxFractionDigits: Int = 2) -> String? {
        let formatter = NumberFormatter()
        formatter.minimumFractionDigits = minFractionDigits
        formatter.maximumFractionDigits = maxFractionDigits
        return formatter.string(from: NSNumber(value: self))
    }
}
