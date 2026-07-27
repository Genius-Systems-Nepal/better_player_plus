Pod::Spec.new do |s|
  s.name             = 'QuanteecIOS'
  s.version          = '1.0.26'
  s.summary          = 'Quanteec P2P SDK for iOS AVPlayer'
  s.description      = 'Vended Quanteec xcframeworks from https://github.com/Quanteec/quanteec_ios_package'
  s.homepage         = 'https://quanteec.com'
  s.license          = { :type => 'Commercial', :text => 'Quanteec SDK — see quanteec.com' }
  s.author           = { 'Quanteec' => 'support@quanteec.com' }
  s.source           = {
    :git => 'https://github.com/Quanteec/quanteec_ios_package.git',
    :tag => 'v1.0.26'
  }
  # Matches feat/better-player-plus (SPM exactVersion 1.0.26) app floor of iOS 13.
  s.platform         = :ios, '13.0'
  s.vendored_frameworks = 'Frameworks/QuanteecCore.xcframework',
                          'Frameworks/QuanteecPluginAVPlayer.xcframework',
                          'Frameworks/datachannel_wrapper.xcframework'
end
