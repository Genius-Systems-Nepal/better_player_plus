import 'dart:async';
import 'package:better_player_plus/better_player_plus.dart';
import 'package:better_player_plus/src/subtitles/better_player_subtitle.dart';
import 'package:better_player_plus/src/video_player/video_player.dart';
import 'package:flutter/material.dart';
import 'package:flutter_widget_from_html_core/flutter_widget_from_html_core.dart';

class BetterPlayerSubtitlesDrawer extends StatefulWidget {
  const BetterPlayerSubtitlesDrawer({
    super.key,
    required this.subtitles,
    required this.betterPlayerController,
    this.betterPlayerSubtitlesConfiguration,
    required this.playerVisibilityStream,
  });
  final List<BetterPlayerSubtitle> subtitles;
  final BetterPlayerController betterPlayerController;
  final BetterPlayerSubtitlesConfiguration? betterPlayerSubtitlesConfiguration;
  final Stream<bool> playerVisibilityStream;

  @override
  State<BetterPlayerSubtitlesDrawer> createState() => _BetterPlayerSubtitlesDrawerState();
}

class _BetterPlayerSubtitlesDrawerState extends State<BetterPlayerSubtitlesDrawer> {
  final RegExp htmlRegExp =
      // ignore: unnecessary_raw_strings
      RegExp(r'<[^>]*>', multiLine: true);
  late TextStyle _innerTextStyle;
  late TextStyle _outerTextStyle;

  VideoPlayerValue? _latestValue;
  BetterPlayerSubtitlesConfiguration? _configuration;
  bool _playerVisible = false;

  ///Stream used to detect if play controls are visible or not
  late StreamSubscription<bool> _visibilityStreamSubscription;
  VideoPlayerController? _attachedController;
  VoidCallback? _controllerListener;

  @override
  void initState() {
    _visibilityStreamSubscription = widget.playerVisibilityStream.listen((state) {
      setState(() {
        _playerVisible = state;
      });
    });

    if (widget.betterPlayerSubtitlesConfiguration != null) {
      _configuration = widget.betterPlayerSubtitlesConfiguration;
    } else {
      _configuration = setupDefaultConfiguration();
    }

    _attachControllerListener();

    _outerTextStyle = TextStyle(
      fontSize: _configuration!.fontSize,
      fontFamily: _configuration!.fontFamily,
      foreground: Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = _configuration!.outlineSize
        ..color = _configuration!.outlineColor,
    );

    _innerTextStyle = TextStyle(
      fontFamily: _configuration!.fontFamily,
      color: _configuration!.fontColor,
      fontSize: _configuration!.fontSize,
    );

    super.initState();
  }

  @override
  void didUpdateWidget(BetterPlayerSubtitlesDrawer oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.betterPlayerController.videoPlayerController != widget.betterPlayerController.videoPlayerController) {
      _detachControllerListener();
      _attachControllerListener();
    }
  }

  void _attachControllerListener() {
    _controllerListener ??= _updateState;
    final controller = widget.betterPlayerController.videoPlayerController;
    _attachedController = controller;
    controller?.addListener(_controllerListener!);
    _latestValue = controller?.value;
  }

  void _detachControllerListener() {
    final listener = _controllerListener;
    if (listener != null) {
      _attachedController?.removeListener(listener);
    }
    _attachedController = null;
  }

  @override
  void dispose() {
    _detachControllerListener();
    _visibilityStreamSubscription.cancel();
    super.dispose();
  }

  ///Called when player state has changed, i.e. new player position, etc.
  void _updateState() {
    if (!mounted) {
      return;
    }
    final controller = widget.betterPlayerController.videoPlayerController;
    if (controller == null) {
      return;
    }
    setState(() {
      _latestValue = controller.value;
    });
  }

  @override
  Widget build(BuildContext context) {
    final BetterPlayerSubtitle? subtitle = _getSubtitleAtCurrentPosition();
    widget.betterPlayerController.renderedSubtitle = subtitle;
    final List<String> subtitles = subtitle?.texts ?? [];
    final List<Widget> textWidgets = subtitles.map(_buildSubtitleTextWidget).toList();

    return SizedBox.expand(
      child: Padding(
        padding: EdgeInsets.only(
          bottom: _playerVisible ? _configuration!.bottomPadding + 30 : _configuration!.bottomPadding,
          left: _configuration!.leftPadding,
          right: _configuration!.rightPadding,
        ),
        child: Column(mainAxisAlignment: MainAxisAlignment.end, children: textWidgets),
      ),
    );
  }

  BetterPlayerSubtitle? _getSubtitleAtCurrentPosition() {
    if (_latestValue == null) {
      return null;
    }

    final Duration position = _latestValue!.position;
    for (final BetterPlayerSubtitle subtitle in widget.betterPlayerController.subtitlesLines) {
      if (subtitle.start! <= position && subtitle.end! >= position) {
        return subtitle;
      }
    }
    return null;
  }

  Widget _buildSubtitleTextWidget(String subtitleText) => Row(
    children: [
      Expanded(
        child: Align(alignment: _configuration!.alignment, child: _getTextWithStroke(subtitleText)),
      ),
    ],
  );

  Widget _getTextWithStroke(String subtitleText) => ColoredBox(
    color: _configuration!.backgroundColor,
    child: Stack(
      children: [
        if (_configuration!.outlineEnabled) _buildHtmlWidget(subtitleText, _outerTextStyle) else const SizedBox(),
        _buildHtmlWidget(subtitleText, _innerTextStyle),
      ],
    ),
  );

  Widget _buildHtmlWidget(String text, TextStyle textStyle) => HtmlWidget(text, textStyle: textStyle);

  BetterPlayerSubtitlesConfiguration setupDefaultConfiguration() => const BetterPlayerSubtitlesConfiguration();
}
