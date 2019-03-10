package com.bappstack.flutter_thumbnail_picker;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;


public class FlutterThumbnailPickerPlugin implements MethodCallHandler {
  private static final String CHANNEL = "plugins.flutter.io/flutter_thumbnail_picker";

  private static final int SOURCE_CAMERA = 0;
  private static final int SOURCE_GALLERY = 1;

  private final Registrar registrar;
  private final ImagePickerDelegate delegate;

  public static void registerWith(Registrar registrar) {
    if (registrar.activity() == null) {
      // If a background flutter view tries to register the plugin, there will be no activity from the registrar,
      // we stop the registering process immediately because the ImagePicker requires an activity.
      return;
    }
    final MethodChannel channel = new MethodChannel(registrar.messenger(), CHANNEL);

    final File externalFilesDirectory =
            registrar.activity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
    final ExifDataCopier exifDataCopier = new ExifDataCopier();
    final ImageResizer imageResizer = new ImageResizer(externalFilesDirectory, exifDataCopier);

    final ImagePickerDelegate delegate =
            new ImagePickerDelegate(registrar.activity(), externalFilesDirectory, imageResizer);
    registrar.addActivityResultListener(delegate);
    registrar.addRequestPermissionsResultListener(delegate);

    final FlutterThumbnailPickerPlugin instance = new FlutterThumbnailPickerPlugin(registrar, delegate);
    channel.setMethodCallHandler(instance);
  }

  @VisibleForTesting
  FlutterThumbnailPickerPlugin(Registrar registrar, ImagePickerDelegate delegate) {
    this.registrar = registrar;
    this.delegate = delegate;
  }

  @Override
  public void onMethodCall(MethodCall call, Result result) {

    if (registrar.activity() == null) {
      result.error("no_activity", "image_picker plugin requires a foreground activity.", null);
      return;
    }
    if (call.method.equals("pickImage")) {
      int imageSource = call.argument("source");
      switch (imageSource) {
        case SOURCE_GALLERY:
          delegate.chooseImageFromGallery(call, result);
          break;
        case SOURCE_CAMERA:
          delegate.takeImageWithCamera(call, result);
          break;
        default:
          throw new IllegalArgumentException("Invalid image source: " + imageSource);
      }
    } else if (call.method.equals("pickVideo")) {
      int imageSource = call.argument("source");
      switch (imageSource) {
        case SOURCE_GALLERY:
          delegate.chooseVideoFromGallery(call, result);
          break;
        case SOURCE_CAMERA:
          delegate.takeVideoWithCamera(call, result);
          break;
        default:
          throw new IllegalArgumentException("Invalid video source: " + imageSource);
      }
    } else if (call.method.equals("generateImageThumbnail")) {
      delegate.generateImageThumbnail(call, result);
    } else if (call.method.equals("generateVideoThumbnail")) {
      //delegate.generateVideoThumbnail(call, result);
      Map<String, Object> arguments = call.arguments();
      String videoFile = (String) arguments.get("videoFilePath");
      String thumbOutputFile = (String) arguments.get("thumbFilePath");
      int imageType = (int) arguments.get("thumbnailFormat");
      int quality = (int) arguments.get("thumbnailQuality");
      try {
        String thumbnailPath = buildThumbnail(videoFile, thumbOutputFile, imageType, quality);
        Log.e("Maugost",videoFile+"Hy me");

        result.success(thumbnailPath);
      } catch (Throwable throwable) {
        throwable.printStackTrace();
        Log.e("Maugost",throwable.getMessage()+"Hy me");

      }
    } else {
      throw new IllegalArgumentException("Unknown method " + call.method);
    }
  }



  private String buildThumbnail(String vidPath, String thumbPath, int type, int quality) {
    if (vidPath == null || vidPath.equals("")) {
      Log.println(Log.INFO, "WARNING", "Thumbnails: Video Path must not be null or empty!");
      return null;
    }
    return thumbPath == null ? cacheDirectory(vidPath, type, quality) : userDirectory(vidPath, thumbPath, type, quality);

  }

  private String cacheDirectory(String vidPath, int type, int quality) {
    Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(vidPath, MediaStore.Video.Thumbnails.MINI_KIND);
    String sourceFileName = getFileName(Uri.parse(vidPath).getLastPathSegment());
    File tempDir = new File(this.registrar.context().getExternalCacheDir() + File.separator + "ThumbFiles" + File.separator);
    if (tempDir.exists()) {
      clearThumbnails();
    } else {
      tempDir.mkdirs();
    }
    String tempFile = new File(tempDir + File.separator + sourceFileName).getPath();
    switch (type) {
      case 1:
        try {
          FileOutputStream out = new FileOutputStream(new File(tempFile + ".jpg"));
          bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
          out.flush();
          out.close();
          return tempFile + ".jpg";
        } catch (IOException e) {
          e.printStackTrace();
        }
      case 2:
        try {
          FileOutputStream out = new FileOutputStream(new File(tempFile + ".png"));
          bitmap.compress(Bitmap.CompressFormat.PNG, quality, out);
          out.flush();
          out.close();
          return tempFile + ".png";
        } catch (IOException e) {
          e.printStackTrace();
        }
      case 3:
        try {
          FileOutputStream out = new FileOutputStream(new File(tempFile + ".webp"));
          bitmap.compress(Bitmap.CompressFormat.WEBP, quality, out);
          out.flush();
          out.close();
          return tempFile + ".webp";
        } catch (IOException e) {
          e.printStackTrace();
        }
    }
    return "";
  }

  private String userDirectory(String vidPath, String thumbPath, int type, int quality) {
    File fileDir = null;
    Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(vidPath, MediaStore.Video.Thumbnails.MINI_KIND);
    String sourceFileName = getFileName(Uri.parse(vidPath).getLastPathSegment());
    fileDir = new File(thumbPath + File.separator);
    if (!fileDir.exists()) {
      fileDir.mkdirs();
    }
    String tempFile = new File(fileDir + File.separator + sourceFileName).getAbsolutePath();
    switch (type) {
      case 1:
        try {
          FileOutputStream out = new FileOutputStream(new File(tempFile + ".jpg"));
          bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
          out.flush();
          out.close();
          return tempFile + ".jpg";
        } catch (IOException e) {
          e.printStackTrace();
        }
        break;

      case 2:
        try {
          FileOutputStream out = new FileOutputStream(new File(tempFile + ".png"));
          bitmap.compress(Bitmap.CompressFormat.PNG, quality, out);
          out.flush();
          out.close();
          return tempFile + ".png";
        } catch (IOException e) {
          e.printStackTrace();
        }
        break;
      case 3:
        try {
          FileOutputStream out = new FileOutputStream(new File(tempFile + "webp"));
          bitmap.compress(Bitmap.CompressFormat.WEBP, quality, out);
          out.flush();
          out.close();
          return tempFile + ".webp";
        } catch (IOException e) {
          e.printStackTrace();
        }
        break;
    }
    return "";
  }

  private void clearThumbnails() {
    String tempDirPath = this.registrar.context().getExternalCacheDir()
            + File.separator + "TempFiles" + File.separator;
    File tempDir = new File(tempDirPath);
    if (tempDir.exists()) {
      String[] children = tempDir.list();
      for (String file : children) {
        new File(tempDir, file).delete();
      }
    }
  }

  private static final Pattern ext = Pattern.compile("(?<=.)\\.[^.]+$");

  private String getFileName(String s) {
    return ext.matcher(s).replaceAll("");
  }



}
