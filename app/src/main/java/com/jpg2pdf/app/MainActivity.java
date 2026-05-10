package com.jpg2pdf.app;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private AutoCompleteTextView imageSourceSpinner;
    private AutoCompleteTextView savePathSpinner;
    private AutoCompleteTextView loadCountSpinner;
    private TextInputEditText etFilename;
    private TextInputEditText etCustomCount;
    private LinearLayout customCountContainer;
    private TextView tvImageCount;
    private TextView tvImagePath;
    private TextView tvSavePath;
    private TextView tvProgress;
    private RecyclerView rvThumbnails;
    private MaterialCardView thumbnailCard;
    private MaterialCardView cropCard;
    private MaterialButton btnSelectImages;
    private MaterialButton btnLoadImages;
    private MaterialButton btnGeneratePdf;
    private MaterialButton btnSharePdf;
    private MaterialButton btnAddCropBox;
    private MaterialButton btnAddWhiteBox;
    private MaterialButton btnRemoveWhiteBox;
    private MaterialCheckBox cbDeleteOriginal;
    private LinearLayout progressContainer;
    private LinearProgressIndicator progressBar;
    private ImageView ivCropImage;
    private CropOverlayView cropOverlay;

    private final List<Uri> selectedImages = new ArrayList<>();
    private final List<String> selectedImagePaths = new ArrayList<>();
    private final List<RectF> cropRects = new ArrayList<>();
    private final List<RectF> whiteBoxRects = new ArrayList<>();
    private ThumbnailAdapter thumbnailAdapter;
    private File lastGeneratedPdf;
    private Uri lastGeneratedPdfUri;
    private Uri customImageSourceTreeUri;
    private Uri customSavePathTreeUri;

    private static final String[] IMAGE_SOURCES = {"截图", "相册", "下载", "自定义路径"};
    private static final String[] SAVE_PATHS = {"下载", "截图", "照片", "自定义路径"};

    private static final int SOURCE_SCREENSHOTS = 0;
    private static final int SOURCE_GALLERY = 1;
    private static final int SOURCE_DOWNLOADS = 2;
    private static final int SOURCE_CUSTOM = 3;

    private static final int PATH_DOWNLOADS = 0;
    private static final int PATH_SCREENSHOTS = 1;
    private static final int PATH_PICTURES = 2;
    private static final int PATH_CUSTOM = 3;

    private int selectedImageSource = SOURCE_SCREENSHOTS;
    private int selectedSavePath = PATH_DOWNLOADS;
    private int selectedCropImageIndex = -1;
    private int selectedLoadCount = 10;
    private boolean useCustomLoadCount = false;
    private float savedCropLeft = -1f;
    private float savedCropTop = -1f;
    private float savedCropRight = -1f;
    private float savedCropBottom = -1f;

    private static final int REQUEST_PERMISSIONS = 100;
    private static final String PREFS_NAME = "Jpg2PdfPrefs";
    private static final String KEY_SAVE_PATH_URI = "save_path_uri";
    private static final String KEY_SAVE_PATH_INDEX = "save_path_index";
    private static final String KEY_IMAGE_SOURCE_URI = "image_source_uri";
    private static final String KEY_IMAGE_SOURCE_INDEX = "image_source_index";
    private static final String KEY_CROP_LEFT = "crop_left";
    private static final String KEY_CROP_TOP = "crop_top";
    private static final String KEY_CROP_RIGHT = "crop_right";
    private static final String KEY_CROP_BOTTOM = "crop_bottom";

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> folderPickerLauncher;

    private boolean pickingCustomImageSource = false;
    private boolean pickingCustomSavePath = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        loadPreferences();
        initSpinners();
        initRecyclerView();
        initCropView();
        initLaunchers();
        requestPermissions();
        requestManageStorage();
    }

    private void requestManageStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void initViews() {
        imageSourceSpinner = findViewById(R.id.imageSourceSpinner);
        savePathSpinner = findViewById(R.id.savePathSpinner);
        loadCountSpinner = findViewById(R.id.loadCountSpinner);
        etFilename = findViewById(R.id.etFilename);
        etCustomCount = findViewById(R.id.etCustomCount);
        customCountContainer = findViewById(R.id.customCountContainer);
        tvImageCount = findViewById(R.id.tvImageCount);
        tvImagePath = findViewById(R.id.tvImagePath);
        tvSavePath = findViewById(R.id.tvSavePath);
        tvProgress = findViewById(R.id.tvProgress);
        rvThumbnails = findViewById(R.id.rvThumbnails);
        thumbnailCard = findViewById(R.id.thumbnailCard);
        cropCard = findViewById(R.id.cropCard);
        btnSelectImages = findViewById(R.id.btnSelectImages);
        btnLoadImages = findViewById(R.id.btnLoadImages);
        btnGeneratePdf = findViewById(R.id.btnGeneratePdf);
        btnSharePdf = findViewById(R.id.btnSharePdf);
        btnAddCropBox = findViewById(R.id.btnAddCropBox);
        btnAddWhiteBox = findViewById(R.id.btnAddWhiteBox);
        btnRemoveWhiteBox = findViewById(R.id.btnRemoveWhiteBox);
        cbDeleteOriginal = findViewById(R.id.cbDeleteOriginal);
        progressContainer = findViewById(R.id.progressContainer);
        progressBar = findViewById(R.id.progressBar);
        ivCropImage = findViewById(R.id.ivCropImage);
        cropOverlay = findViewById(R.id.cropOverlay);

        btnSelectImages.setOnClickListener(v -> pickImages());
        btnLoadImages.setOnClickListener(v -> loadLatestImages());
        btnGeneratePdf.setOnClickListener(v -> generatePdf());
        btnSharePdf.setOnClickListener(v -> sharePdf());
        btnAddCropBox.setOnClickListener(v -> addCropBox());
        btnAddWhiteBox.setOnClickListener(v -> addWhiteBox());
        btnRemoveWhiteBox.setOnClickListener(v -> removeWhiteBox());
        cbDeleteOriginal.setChecked(true);
        etFilename.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && etFilename.getText().toString().isEmpty()) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                if (clipboard != null && clipboard.hasPrimaryClip()) {
                    ClipData clip = clipboard.getPrimaryClip();
                    if (clip != null && clip.getItemCount() > 0) {
                        CharSequence text = clip.getItemAt(0).getText();
                        if (text != null && text.length() > 0) {
                            etFilename.setText(text.toString().trim());
                        }
                    }
                }
            }
        });
    }

    private void initSpinners() {
        ArrayAdapter<String> imageSourceAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, IMAGE_SOURCES);
        imageSourceSpinner.setAdapter(imageSourceAdapter);
        imageSourceSpinner.setText(IMAGE_SOURCES[selectedImageSource], false);
        imageSourceSpinner.setOnItemClickListener((parent, view, position, id) -> {
            selectedImageSource = position;
            if (selectedImageSource == SOURCE_CUSTOM) {
                pickCustomImageSourceFolder();
            } else {
                customImageSourceTreeUri = null;
            }
            updateImagePathDisplay();
        });

        ArrayAdapter<String> savePathAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, SAVE_PATHS);
        savePathSpinner.setAdapter(savePathAdapter);
        savePathSpinner.setText(SAVE_PATHS[selectedSavePath], false);
        savePathSpinner.setOnItemClickListener((parent, view, position, id) -> {
            selectedSavePath = position;
            if (selectedSavePath == PATH_CUSTOM) {
                pickCustomSaveFolder();
            } else {
                customSavePathTreeUri = null;
            }
            updateSavePathDisplay();
        });

        String[] loadCountItems = new String[41];
        for (int i = 1; i <= 40; i++) {
            loadCountItems[i - 1] = String.valueOf(i);
        }
        loadCountItems[40] = "自定义";
        ArrayAdapter<String> loadCountAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, loadCountItems);
        loadCountSpinner.setAdapter(loadCountAdapter);
        loadCountSpinner.setText("10", false);
        loadCountSpinner.setOnItemClickListener((parent, view, position, id) -> {
            if (position == 40) {
                useCustomLoadCount = true;
                customCountContainer.setVisibility(View.VISIBLE);
            } else {
                useCustomLoadCount = false;
                selectedLoadCount = position + 1;
                customCountContainer.setVisibility(View.GONE);
            }
        });

        updateSavePathDisplay();
        updateImagePathDisplay();
    }

    private void initRecyclerView() {
        thumbnailAdapter = new ThumbnailAdapter();
        rvThumbnails.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvThumbnails.setAdapter(thumbnailAdapter);

        thumbnailAdapter.setOnItemClickListener(position -> {
            selectedCropImageIndex = position;
            updateCropImage();
            cropCard.setVisibility(View.VISIBLE);
        });

        thumbnailAdapter.setOnItemDeleteListener(this::deleteImageAt);
    }

    private void deleteImageAt(int position) {
        if (position < 0 || position >= selectedImages.size()) return;

        selectedImages.remove(position);
        selectedImagePaths.remove(position);
        thumbnailAdapter.removeImage(position);

        if (selectedImages.isEmpty()) {
            thumbnailCard.setVisibility(View.GONE);
            cropCard.setVisibility(View.GONE);
            tvImageCount.setText("");
            selectedCropImageIndex = -1;
        } else {
            tvImageCount.setText(getString(R.string.images_count, selectedImages.size()));
            if (selectedCropImageIndex >= selectedImages.size()) {
                selectedCropImageIndex = selectedImages.size() - 1;
            }
            if (selectedCropImageIndex >= 0) {
                updateCropImage();
            }
        }
    }

    private void initCropView() {
        cropOverlay.post(() -> {
            if (!cropOverlay.hasCropRect()) {
                cropOverlay.initDefaultCropRect();
            }
        });
    }

    private void initLaunchers() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        handleImageResult(result.getData());
                    }
                });

        folderPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            if (pickingCustomImageSource) {
                                customImageSourceTreeUri = uri;
                                getContentResolver().takePersistableUriPermission(uri,
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                pickingCustomImageSource = false;
                                updateImagePathDisplay();
                            } else if (pickingCustomSavePath) {
                                customSavePathTreeUri = uri;
                                getContentResolver().takePersistableUriPermission(uri,
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                                pickingCustomSavePath = false;
                                updateSavePathDisplay();
                            }
                        }
                    }
                });
    }

    private void requestPermissions() {
        List<String> permissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES);
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        List<String> needRequest = new ArrayList<>();
        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                needRequest.add(perm);
            }
        }
        if (!needRequest.isEmpty()) {
            requestPermissions(needRequest.toArray(new String[0]), REQUEST_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show();
                    break;
                }
            }
        }
    }

    private void pickImages() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setType("image/*");
        String title;
        switch (selectedImageSource) {
            case SOURCE_SCREENSHOTS:
                title = "选择截图";
                break;
            case SOURCE_DOWNLOADS:
                title = "选择图片";
                break;
            case SOURCE_CUSTOM:
                pickCustomImageSourceFolder();
                return;
            default:
                title = "选择图片";
                break;
        }
        imagePickerLauncher.launch(Intent.createChooser(intent, title));
    }

    private void loadLatestImages() {
        int count;
        if (useCustomLoadCount) {
            String s = etCustomCount.getText().toString().trim();
            if (s.isEmpty()) {
                Toast.makeText(this, "请输入自定义张数", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                count = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "请输入有效数字", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            count = selectedLoadCount;
        }

        if (count <= 0) {
            Toast.makeText(this, "加载张数必须大于0", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Uri> latestUris = getLatestImages(count);
        if (latestUris.isEmpty()) {
            Toast.makeText(this, "未找到图片，请确认路径下有图片文件", Toast.LENGTH_LONG).show();
            return;
        }

        for (int i = latestUris.size() - 1; i >= 0; i--) {
            Uri uri = latestUris.get(i);
            selectedImages.add(uri);
            selectedImagePaths.add(getRealPathFromUri(uri));
            cropRects.add(null);
            whiteBoxRects.add(null);
        }

        updateThumbnailDisplay();
        Toast.makeText(this, "已加载 " + latestUris.size() + " 张图片", Toast.LENGTH_SHORT).show();
    }

    private List<Uri> getLatestImages(int count) {
        List<ImageInfo> imageList = new ArrayList<>();

        if (selectedImageSource == SOURCE_CUSTOM && customImageSourceTreeUri != null) {
            DocumentFile treeDoc = DocumentFile.fromTreeUri(this, customImageSourceTreeUri);
            if (treeDoc != null && treeDoc.isDirectory()) {
                for (DocumentFile file : treeDoc.listFiles()) {
                    if (file.isFile() && file.getName() != null && isImageFile(file.getName())) {
                        imageList.add(new ImageInfo(file.getUri(), file.lastModified()));
                    }
                }
                Collections.sort(imageList, (a, b) -> Long.compare(b.dateModified, a.dateModified));
                List<Uri> result = new ArrayList<>();
                for (int i = 0; i < Math.min(count, imageList.size()); i++) {
                    result.add(imageList.get(i).uri);
                }
                return result;
            }
        }

        ContentResolver resolver = getContentResolver();
        Uri collection;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        } else {
            collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        }

        File sourceDir = getImageSourceDirectory();
        String folderPath = sourceDir.getAbsolutePath();

        if (sourceDir.exists() && sourceDir.isDirectory()) {
            File[] files = sourceDir.listFiles((dir, name) -> isImageFile(name));
            if (files != null && files.length > 0) {
                for (File file : files) {
                    imageList.add(new ImageInfo(Uri.fromFile(file), file.lastModified()));
                }
                Collections.sort(imageList, (a, b) -> Long.compare(b.dateModified, a.dateModified));
                List<Uri> result = new ArrayList<>();
                for (int i = 0; i < Math.min(count, imageList.size()); i++) {
                    result.add(imageList.get(i).uri);
                }
                return result;
            }
        }

        String basePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        String relativeTargetPath = "";
        if (folderPath.startsWith(basePath)) {
            relativeTargetPath = folderPath.substring(basePath.length());
            if (relativeTargetPath.startsWith("/")) {
                relativeTargetPath = relativeTargetPath.substring(1);
            }
            if (!relativeTargetPath.endsWith("/")) {
                relativeTargetPath = relativeTargetPath + "/";
            }
        }

        String[] projection;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projection = new String[]{
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.RELATIVE_PATH,
                    MediaStore.Images.Media.DATE_ADDED
            };
        } else {
            projection = new String[]{
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media.DATE_ADDED
            };
        }

        Cursor cursor = null;
        try {
            cursor = resolver.query(collection, projection, null, null,
                    MediaStore.Images.Media.DATE_ADDED + " DESC");
            if (cursor != null) {
                int idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                int dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idCol);
                    long dateAdded = cursor.getLong(dateCol);
                    boolean match = false;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        int relCol = cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH);
                        if (relCol >= 0) {
                            String relPath = cursor.getString(relCol);
                            if (relPath != null) {
                                match = relPath.equals(relativeTargetPath)
                                        || relPath.startsWith(relativeTargetPath);
                            }
                        }
                    } else {
                        int dataCol = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                        if (dataCol >= 0) {
                            String dataPath = cursor.getString(dataCol);
                            if (dataPath != null) {
                                match = dataPath.startsWith(folderPath);
                            }
                        }
                    }

                    if (match) {
                        Uri uri = Uri.withAppendedPath(collection, String.valueOf(id));
                        imageList.add(new ImageInfo(uri, dateAdded));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }

        Collections.sort(imageList, (a, b) -> Long.compare(b.dateModified, a.dateModified));
        List<Uri> result = new ArrayList<>();
        for (int i = 0; i < Math.min(count, imageList.size()); i++) {
            result.add(imageList.get(i).uri);
        }
        return result;
    }

    private boolean isImageFile(String name) {
        String lower = name.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                lower.endsWith(".png") || lower.endsWith(".gif") ||
                lower.endsWith(".bmp") || lower.endsWith(".webp");
    }

    private static class ImageInfo {
        Uri uri;
        long dateModified;
        ImageInfo(Uri uri, long dateModified) {
            this.uri = uri;
            this.dateModified = dateModified;
        }
    }

    private void handleImageResult(Intent data) {
        if (data.getClipData() != null) {
            int cnt = data.getClipData().getItemCount();
            for (int i = 0; i < cnt; i++) {
                Uri uri = data.getClipData().getItemAt(i).getUri();
                selectedImages.add(uri);
                selectedImagePaths.add(getRealPathFromUri(uri));
                cropRects.add(null);
                whiteBoxRects.add(null);
            }
        } else if (data.getData() != null) {
            Uri uri = data.getData();
            selectedImages.add(uri);
            selectedImagePaths.add(getRealPathFromUri(uri));
            cropRects.add(null);
            whiteBoxRects.add(null);
        }
        updateThumbnailDisplay();
    }

    private void updateThumbnailDisplay() {
        if (selectedImages.isEmpty()) {
            thumbnailCard.setVisibility(View.GONE);
            cropCard.setVisibility(View.GONE);
            tvImageCount.setText("");
        } else {
            thumbnailCard.setVisibility(View.VISIBLE);
            thumbnailAdapter.setImages(selectedImages);
            tvImageCount.setText(getString(R.string.images_count, selectedImages.size()));

            if (selectedCropImageIndex < 0 || selectedCropImageIndex >= selectedImages.size()) {
                selectedCropImageIndex = 0;
            }

            cropCard.setVisibility(View.VISIBLE);
            updateCropImage();
        }
    }

    private void updateCropImage() {
        if (selectedCropImageIndex < 0 || selectedCropImageIndex >= selectedImages.size()) return;

        saveCurrentBoxRects();

        Uri uri = selectedImages.get(selectedCropImageIndex);
        ivCropImage.setImageURI(uri);

        cropOverlay.post(() -> {
            RectF savedCrop = cropRects.get(selectedCropImageIndex);
            RectF savedWhite = whiteBoxRects.get(selectedCropImageIndex);

            if (savedCrop != null) {
                int[] imageSize = getImageSize(uri);
                if (imageSize[0] > 0 && imageSize[1] > 0) {
                    RectF viewRect = cropOverlay.getCropRectInViewCoords(savedCrop,
                            imageSize[0], imageSize[1],
                            ivCropImage.getWidth(), ivCropImage.getHeight());
                    if (viewRect != null) {
                        cropOverlay.setCropRect(viewRect);
                    }
                }
            } else {
                autoAddCropBox(uri);
            }

            if (savedWhite != null) {
                int[] imageSize = getImageSize(uri);
                if (imageSize[0] > 0 && imageSize[1] > 0) {
                    RectF viewRect = cropOverlay.getCropRectInViewCoords(savedWhite,
                            imageSize[0], imageSize[1],
                            ivCropImage.getWidth(), ivCropImage.getHeight());
                    if (viewRect != null) {
                        cropOverlay.setWhiteRect(viewRect);
                    }
                }
            }

            cropOverlay.setActiveMode(CropOverlayView.MODE_CROP);
        });

        cropCard.setVisibility(View.VISIBLE);
    }

    private void saveCurrentBoxRects() {
        if (selectedCropImageIndex < 0) return;
        Uri currentUri = selectedImages.get(selectedCropImageIndex);
        int[] imageSize = getImageSize(currentUri);
        if (imageSize[0] <= 0 || imageSize[1] <= 0) return;

        if (cropOverlay.hasCropRect()) {
            RectF imageRect = cropOverlay.getCropRectInImageCoords(
                    imageSize[0], imageSize[1],
                    ivCropImage.getWidth(), ivCropImage.getHeight());
            cropRects.set(selectedCropImageIndex, imageRect);
        }

        if (cropOverlay.hasWhiteRect()) {
            RectF imageRect = cropOverlay.getWhiteRectInImageCoords(
                    imageSize[0], imageSize[1],
                    ivCropImage.getWidth(), ivCropImage.getHeight());
            whiteBoxRects.set(selectedCropImageIndex, imageRect);
        }
    }

    private void addCropBox() {
        cropOverlay.setActiveMode(CropOverlayView.MODE_CROP);
        cropOverlay.initDefaultCropRect();
    }

    private void addWhiteBox() {
        cropOverlay.setActiveMode(CropOverlayView.MODE_WHITE_BOX);
        cropOverlay.initDefaultWhiteRect();
    }

    private void removeWhiteBox() {
        cropOverlay.clearWhiteRect();
        if (selectedCropImageIndex >= 0) {
            whiteBoxRects.set(selectedCropImageIndex, null);
        }
    }

    private void autoAddCropBox(Uri uri) {
        if (savedCropLeft >= 0 && ivCropImage.getWidth() > 0) {
            int[] imageSize = getImageSize(uri);
            if (imageSize[0] > 0 && imageSize[1] > 0) {
                RectF imgRect = new RectF(
                        savedCropLeft * imageSize[0],
                        savedCropTop * imageSize[1],
                        savedCropRight * imageSize[0],
                        savedCropBottom * imageSize[1]
                );
                RectF viewRect = cropOverlay.getCropRectInViewCoords(imgRect,
                        imageSize[0], imageSize[1],
                        ivCropImage.getWidth(), ivCropImage.getHeight());
                if (viewRect != null) {
                    cropOverlay.setCropRect(viewRect);
                    return;
                }
            }
        }
        cropOverlay.initDefaultCropRect();
    }

    private void saveCropRectPrefs() {
        if (cropOverlay.hasCropRect() && selectedCropImageIndex >= 0
                && selectedCropImageIndex < selectedImages.size()) {
            Uri currentUri = selectedImages.get(selectedCropImageIndex);
            int[] imageSize = getImageSize(currentUri);
            if (imageSize[0] > 0 && imageSize[1] > 0) {
                RectF imageRect = cropOverlay.getCropRectInImageCoords(
                        imageSize[0], imageSize[1],
                        ivCropImage.getWidth(), ivCropImage.getHeight());
                if (imageRect != null) {
                    savedCropLeft = imageRect.left / imageSize[0];
                    savedCropTop = imageRect.top / imageSize[1];
                    savedCropRight = imageRect.right / imageSize[0];
                    savedCropBottom = imageRect.bottom / imageSize[1];
                    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                    prefs.edit()
                            .putFloat(KEY_CROP_LEFT, savedCropLeft)
                            .putFloat(KEY_CROP_TOP, savedCropTop)
                            .putFloat(KEY_CROP_RIGHT, savedCropRight)
                            .putFloat(KEY_CROP_BOTTOM, savedCropBottom)
                            .apply();
                }
            }
        }
    }

    private void pickCustomImageSourceFolder() {
        pickingCustomImageSource = true;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        folderPickerLauncher.launch(intent);
    }

    private void pickCustomSaveFolder() {
        pickingCustomSavePath = true;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        folderPickerLauncher.launch(intent);
    }

    private void updateImagePathDisplay() {
        if (selectedImageSource == SOURCE_CUSTOM && customImageSourceTreeUri != null) {
            DocumentFile docFile = DocumentFile.fromTreeUri(this, customImageSourceTreeUri);
            String displayName = "未知文件夹";
            if (docFile != null && docFile.getName() != null) {
                displayName = docFile.getName();
            }
            tvImagePath.setText("图片来源: 自定义 - " + displayName);
        } else {
            File dir = getImageSourceDirectory();
            tvImagePath.setText("图片来源: " + dir.getAbsolutePath());
        }
    }

    private void updateSavePathDisplay() {
        if (selectedSavePath == PATH_CUSTOM && customSavePathTreeUri != null) {
            DocumentFile docFile = DocumentFile.fromTreeUri(this, customSavePathTreeUri);
            String displayName = "未知文件夹";
            if (docFile != null && docFile.getName() != null) {
                displayName = docFile.getName();
            }
            tvSavePath.setText("保存路径: 自定义 - " + displayName);
        } else {
            File dir = getSaveDirectory();
            tvSavePath.setText("保存路径: " + dir.getAbsolutePath());
        }
    }

    private File getImageSourceDirectory() {
        switch (selectedImageSource) {
            case SOURCE_SCREENSHOTS:
                return Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DCIM + "/Screenshots");
            case SOURCE_GALLERY:
                return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            case SOURCE_DOWNLOADS:
                return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            case SOURCE_CUSTOM:
                return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
            default:
                return Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DCIM + "/Screenshots");
        }
    }

    private File getSaveDirectory() {
        switch (selectedSavePath) {
            case PATH_DOWNLOADS:
                return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            case PATH_SCREENSHOTS:
                return Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DCIM + "/Screenshots");
            case PATH_PICTURES:
                return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            case PATH_CUSTOM:
                if (customSavePathTreeUri != null) {
                    DocumentFile docFile = DocumentFile.fromTreeUri(this, customSavePathTreeUri);
                    if (docFile != null && docFile.getUri() != null) {
                        String treePath = docFile.getUri().toString();
                        if (treePath.contains("primary:")) {
                            String relativePath = treePath.substring(treePath.indexOf("primary:") + 8);
                            int colonIdx = relativePath.indexOf(':');
                            if (colonIdx > 0) {
                                relativePath = relativePath.substring(colonIdx + 1);
                            }
                            return new File(Environment.getExternalStorageDirectory(), relativePath);
                        }
                    }
                }
                return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            default:
                return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        }
    }

    private void generatePdf() {
        if (selectedImages.isEmpty()) {
            Toast.makeText(this, R.string.no_images_selected, Toast.LENGTH_SHORT).show();
            return;
        }

        String filename = etFilename.getText().toString().trim();
        filename = filename.replaceAll("[\\s\\n\\r\\t]+", "");
        if (filename.isEmpty()) {
            Toast.makeText(this, "请输入PDF文件名", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!filename.endsWith(".pdf")) {
            filename += ".pdf";
        }

        File saveDir = getSaveDirectory();
        if (!saveDir.exists()) {
            saveDir.mkdirs();
        }

        final File tempFile = new File(getCacheDir(), filename);
        final File finalOutputFile = new File(saveDir, filename);

        saveCurrentBoxRects();
        saveCropRectPrefs();

        final List<RectF> finalCropRects = new ArrayList<>();
        RectF currentCrop = null;
        if (selectedCropImageIndex >= 0 && selectedCropImageIndex < cropRects.size()) {
            currentCrop = cropRects.get(selectedCropImageIndex);
        }
        for (int i = 0; i < selectedImages.size(); i++) {
            finalCropRects.add(currentCrop);
        }
        final List<RectF> finalWhiteBoxRects = new ArrayList<>(whiteBoxRects);
        final boolean deleteOriginal = cbDeleteOriginal.isChecked();
        final List<Uri> imagesToDelete = deleteOriginal ? new ArrayList<>(selectedImages) : null;

        progressContainer.setVisibility(View.VISIBLE);
        progressBar.setMax(selectedImages.size());
        progressBar.setProgress(0);
        btnGeneratePdf.setEnabled(false);

        final String finalFilename = filename;

        new Thread(() -> {
            final boolean[] result = {false};
            if (selectedSavePath == PATH_CUSTOM && customSavePathTreeUri != null) {
                result[0] = generatePdfToTreeUri(finalCropRects, finalWhiteBoxRects, tempFile, finalFilename);
            } else {
                result[0] = PdfGenerator.generatePdf(
                        MainActivity.this,
                        selectedImages,
                        finalCropRects,
                        finalWhiteBoxRects,
                        tempFile,
                        (current, total) -> runOnUiThread(() -> {
                            progressBar.setProgress(current);
                            tvProgress.setText(getString(R.string.progress_format, current, total));
                        })
                );
                if (result[0]) {
                    try {
                        InputStream is = new java.io.FileInputStream(tempFile);
                        OutputStream os = new FileOutputStream(finalOutputFile);
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = is.read(buffer)) > 0) {
                            os.write(buffer, 0, len);
                        }
                        os.close();
                        is.close();
                        lastGeneratedPdf = finalOutputFile;
                    } catch (Exception e) {
                        e.printStackTrace();
                        result[0] = false;
                    }
                }
            }

            if (result[0] && deleteOriginal && imagesToDelete != null) {
                for (int i = 0; i < imagesToDelete.size(); i++) {
                    String path = (i < selectedImagePaths.size()) ? selectedImagePaths.get(i) : null;
                    if (path != null) {
                        try {
                            File file = new File(path);
                            if (file.exists()) {
                                file.delete();
                                continue;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    deleteImageFile(imagesToDelete.get(i));
                }
            }

            final boolean finalResult = result[0];
            runOnUiThread(() -> {
                btnGeneratePdf.setEnabled(true);
                if (finalResult) {
                    Toast.makeText(this, R.string.pdf_generated, Toast.LENGTH_SHORT).show();
                    btnSharePdf.setEnabled(true);
                    if (deleteOriginal) {
                        selectedImages.clear();
                        selectedImagePaths.clear();
                        cropRects.clear();
                        whiteBoxRects.clear();
                        thumbnailAdapter.clear();
                        thumbnailCard.setVisibility(View.GONE);
                        cropCard.setVisibility(View.GONE);
                        tvImageCount.setText("");
                        selectedCropImageIndex = -1;
                    }
                } else {
                    Toast.makeText(this, R.string.pdf_generation_failed, Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private boolean generatePdfToTreeUri(List<RectF> cropRects, List<RectF> whiteBoxRects,
                                          File tempFile, String filename) {
        DocumentFile treeDoc = DocumentFile.fromTreeUri(this, customSavePathTreeUri);
        if (treeDoc == null || !treeDoc.isDirectory()) return false;

        DocumentFile existingFile = treeDoc.findFile(filename);
        if (existingFile != null) {
            existingFile.delete();
        }

        DocumentFile pdfFile = treeDoc.createFile("application/pdf", filename);
        if (pdfFile == null) return false;

        boolean success = PdfGenerator.generatePdf(
                MainActivity.this,
                selectedImages,
                cropRects,
                whiteBoxRects,
                tempFile,
                (current, total) -> runOnUiThread(() -> {
                    progressBar.setProgress(current);
                    tvProgress.setText(getString(R.string.progress_format, current, total));
                })
        );

        if (success) {
            try (InputStream is = new java.io.FileInputStream(tempFile);
                 OutputStream os = getContentResolver().openOutputStream(pdfFile.getUri())) {
                if (os != null) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = is.read(buffer)) > 0) {
                        os.write(buffer, 0, len);
                    }
                    os.flush();
                    os.close();
                }
                lastGeneratedPdfUri = pdfFile.getUri();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            } finally {
                tempFile.delete();
            }
        }

        return success;
    }

    private int[] getImageSize(Uri uri) {
        int[] size = new int[]{0, 0};
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            if (is != null) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(is, null, options);
                size[0] = options.outWidth;
                size[1] = options.outHeight;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return size;
    }

    /**
     * 使用多种方法彻底删除图片文件
     */
    private void deleteImageFile(Uri uri) {
        if (uri == null) return;

        try {
            if ("file".equals(uri.getScheme())) {
                File file = new File(uri.getPath());
                if (file.exists()) {
                    boolean deleted = file.delete();
                    if (deleted) return;
                }
            }

            if ("content".equals(uri.getScheme())) {
                long imageId = -1;
                try {
                    String lastSeg = uri.getLastPathSegment();
                    if (lastSeg != null) {
                        imageId = Long.parseLong(lastSeg);
                    }
                } catch (NumberFormatException e) {
                    imageId = -1;
                }

                if (imageId > 0) {
                    Uri mediaUri;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        mediaUri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
                    } else {
                        mediaUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    }
                    Uri deleteUri = Uri.withAppendedPath(mediaUri, String.valueOf(imageId));
                    int deleted = getContentResolver().delete(deleteUri, null, null);
                    if (deleted > 0) return;
                }

                int deleted = getContentResolver().delete(uri, null, null);
                if (deleted > 0) return;
            }

            String realPath = getRealPathFromUri(uri);
            if (realPath != null) {
                File file = new File(realPath);
                if (file.exists()) {
                    boolean deleted = file.delete();
                    if (deleted) return;
                }
            }

            try {
                Process process = Runtime.getRuntime().exec("rm -f \"" + realPath + "\"");
                process.waitFor();
                process.destroy();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 从Content URI获取真实文件路径
     */
    private String getRealPathFromUri(Uri uri) {
        if (uri == null) return null;

        if ("file".equals(uri.getScheme())) {
            return uri.getPath();
        }

        Cursor cursor = null;
        try {
            String[] projection = {MediaStore.Images.Media.DATA};
            cursor = getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                return cursor.getString(columnIndex);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    private void loadPreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String saveUriStr = prefs.getString(KEY_SAVE_PATH_URI, null);
        if (saveUriStr != null) {
            try {
                customSavePathTreeUri = Uri.parse(saveUriStr);
                getContentResolver().takePersistableUriPermission(customSavePathTreeUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            } catch (Exception e) {
                customSavePathTreeUri = null;
            }
        }
        selectedSavePath = prefs.getInt(KEY_SAVE_PATH_INDEX, PATH_DOWNLOADS);

        String imageUriStr = prefs.getString(KEY_IMAGE_SOURCE_URI, null);
        if (imageUriStr != null) {
            try {
                customImageSourceTreeUri = Uri.parse(imageUriStr);
                getContentResolver().takePersistableUriPermission(customImageSourceTreeUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception e) {
                customImageSourceTreeUri = null;
            }
        }
        selectedImageSource = prefs.getInt(KEY_IMAGE_SOURCE_INDEX, SOURCE_SCREENSHOTS);

        savedCropLeft = prefs.getFloat(KEY_CROP_LEFT, -1f);
        savedCropTop = prefs.getFloat(KEY_CROP_TOP, -1f);
        savedCropRight = prefs.getFloat(KEY_CROP_RIGHT, -1f);
        savedCropBottom = prefs.getFloat(KEY_CROP_BOTTOM, -1f);
    }

    private void savePreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        if (customSavePathTreeUri != null) {
            editor.putString(KEY_SAVE_PATH_URI, customSavePathTreeUri.toString());
        } else {
            editor.remove(KEY_SAVE_PATH_URI);
        }
        editor.putInt(KEY_SAVE_PATH_INDEX, selectedSavePath);

        if (customImageSourceTreeUri != null) {
            editor.putString(KEY_IMAGE_SOURCE_URI, customImageSourceTreeUri.toString());
        } else {
            editor.remove(KEY_IMAGE_SOURCE_URI);
        }
        editor.putInt(KEY_IMAGE_SOURCE_INDEX, selectedImageSource);

        saveCropRectPrefs();

        editor.apply();
    }

    @Override
    protected void onPause() {
        super.onPause();
        savePreferences();
    }

    private void sharePdf() {
        if (selectedSavePath == PATH_CUSTOM && lastGeneratedPdfUri != null) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, lastGeneratedPdfUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "分享PDF"));
            return;
        }

        if (lastGeneratedPdf == null || !lastGeneratedPdf.exists()) {
            Toast.makeText(this, "请先生成PDF", Toast.LENGTH_SHORT).show();
            return;
        }

        PdfShareHelper.sharePdf(this, lastGeneratedPdf, getPackageName() + ".fileprovider");
    }
}
