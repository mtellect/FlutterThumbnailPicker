// Copyright 2017 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

import 'dart:async';
import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:thumbnails/thumbnails.dart';

/// Specifies the source where the picked image should come from.
enum ImageSourceFrom {
  /// Opens up the device camera, letting the user to take a new picture.
  camera,

  /// Opens the user's photo gallery.
  gallery,
}

class FlutterThumbnailPicker {
  static const MethodChannel _channel =
      MethodChannel('plugins.flutter.io/flutter_thumbnail_picker');

  /// Returns a [File] object pointing to the image that was picked.
  ///
  /// The [source] argument controls where the image comes from. This can
  /// be either [ImageSourceFrom.camera] or [ImageSourceFrom.gallery].
  ///
  /// If specified, the image will be at most [maxWidth] wide and
  /// [maxHeight] tall. Otherwise the image will be returned at it's
  /// original width and height.
  static Future<File> pickImage({
    @required ImageSourceFrom source,
    double maxWidth,
    double maxHeight,
  }) async {
    assert(source != null);

    if (maxWidth != null && maxWidth < 0) {
      throw ArgumentError.value(maxWidth, 'maxWidth cannot be negative');
    }

    if (maxHeight != null && maxHeight < 0) {
      throw ArgumentError.value(maxHeight, 'maxHeight cannot be negative');
    }

    // TODO(amirh): remove this on when the invokeMethod update makes it to stable Flutter.
    // https://github.com/flutter/flutter/issues/26431
    // ignore: strong_mode_implicit_dynamic_method
    final String path = await _channel.invokeMethod(
      'pickImage',
      <String, dynamic>{
        'source': source.index,
        'maxWidth': maxWidth,
        'maxHeight': maxHeight,
      },
    );

    return path == null ? null : File(path);
  }

  static Future<File> pickVideo({
    @required ImageSourceFrom source,
  }) async {
    assert(source != null);

    // TODO(amirh): remove this on when the invokeMethod update makes it to stable Flutter.
    // https://github.com/flutter/flutter/issues/26431
    // ignore: strong_mode_implicit_dynamic_method
    final String path = await _channel.invokeMethod(
      'pickVideo',
      <String, dynamic>{
        'source': source.index,
      },
    );
    return path == null ? null : File(path);
  }

  static Future<PickingResult> pickImageWithThumbnail({
    @required ImageSourceFrom source,
    double maxWidth,
    double maxHeight,
    @required double thumbnailWidth,
    @required double thumbnailHeight,
  }) async {
    assert(source != null);

    if (maxWidth != null && maxWidth < 0) {
      throw ArgumentError.value(maxWidth, 'maxWidth cannot be negative');
    }

    if (maxHeight != null && maxHeight < 0) {
      throw ArgumentError.value(maxHeight, 'maxHeight cannot be negative');
    }

    if (thumbnailWidth != null && thumbnailWidth < 0) {
      throw ArgumentError.value(maxWidth, 'thumbnailWidth cannot be negative');
    }

    if (thumbnailHeight != null && thumbnailHeight < 0) {
      throw ArgumentError.value(
          thumbnailHeight, 'maxHeight cannot be negative');
    }

    final File image = await pickImage(
        source: source, maxHeight: maxHeight, maxWidth: maxWidth);

    //return null if cancel
    if (image == null) {
      return Future<PickingResult>.value(null);
    }

    final File thumbnail = await generateImageThumbnail(
      image: image,
      height: thumbnailHeight,
      width: thumbnailWidth,
    );

    return Future<PickingResult>.value(
        PickingResult(originalFile: image, thumbnail: thumbnail));
  }

  static Future<PickingResult> pickVideoWithThumbnail({
    @required ImageSourceFrom source,
    @required double thumbnailWidth,
    @required double thumbnailHeight,
  }) async {
    assert(source != null);
    if (thumbnailWidth != null && thumbnailWidth < 0) {
      throw ArgumentError.value(
          thumbnailWidth, 'thumbnailWidth cannot be negative');
    }

    if (thumbnailHeight != null && thumbnailHeight < 0) {
      throw ArgumentError.value(
          thumbnailHeight, 'maxHeight cannot be negative');
    }

    final File image = await pickVideo(
      source: source,
    );

    //return null if cancel
    if (image == null) {
      return Future<PickingResult>.value(null);
    }

    final File thumbnail = await generateVideoThumbnail(
      video: image,
      height: thumbnailHeight,
      width: thumbnailWidth,
    );

    return Future<PickingResult>.value(
        PickingResult(originalFile: image, thumbnail: thumbnail));
  }

  static Future<File> generateImageThumbnail({
    @required File image,
    double width,
    double height,
  }) async {
    assert(image != null);

    if (width != null && width < 0) {
      throw ArgumentError.value(width, 'maxWidth cannot be negative');
    }

    if (height != null && height < 0) {
      throw ArgumentError.value(height, 'maxHeight cannot be negative');
    }

    final String path = await _channel.invokeMethod(
      'generateImageThumbnail',
      <String, dynamic>{
        'originalImagePath': image.path,
        'width': width,
        'height': height,
      },
    );
    return path == null ? null : File(path);
  }

  static Future<File> generateVideoThumbnail({
    @required File video,
    double width,
    double height,
  }) async {
    assert(video != null);

    if (width != null && width < 0) {
      throw ArgumentError.value(width, 'maxWidth cannot be negative');
    }

    if (height != null && height < 0) {
      throw ArgumentError.value(height, 'maxHeight cannot be negative');
    }
    String path;
    if (Platform.isIOS) {
      path = await _getIOSThumbnail(video: video, height: height, width: width);
    } else if (Platform.isAndroid) {
      path = await _getAndroidThumbnail(videoPath: video.path);
    }
    return path == null ? null : File(path);
  }

  static Future<String> _getAndroidThumbnail(
      {@required String videoPath}) async {
    String thumb = await Thumbnails.getThumbnail(
        videoFile: videoPath, imageType: ThumbFormat.PNG, quality: 30);
    return thumb;
  }

  static Future<String> _getIOSThumbnail(
      {@required File video, double width, double height}) async {
    var utilMap = <String, dynamic>{
      'originalVideoPath': video.path,
      'width': width,
      'height': height,
    };

    final String thumb = await _channel.invokeMethod(
      'generateVideoThumbnail',
      utilMap,
    );

    return thumb;
  }
}

class PickingResult {
  const PickingResult({@required this.originalFile, @required this.thumbnail});

  ///
  /// original file of video or image
  ///
  final File originalFile;
  final File thumbnail;
}
