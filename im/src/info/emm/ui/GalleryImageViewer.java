/*
 * This is the source code of Emm for Android v. 1.3.2.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013.
 */

package info.emm.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import info.emm.messenger.FileLoader;
import info.emm.messenger.FileLog;
import info.emm.messenger.LocaleController;
import info.emm.messenger.MediaController;
import info.emm.messenger.MessagesController;
import info.emm.messenger.NotificationCenter;
import info.emm.messenger.TLRPC;
import info.emm.messenger.UserConfig;
import info.emm.objects.MessageObject;
import info.emm.objects.PhotoObject;
import info.emm.ui.Views.AbstractGalleryActivity;
import info.emm.ui.Views.GalleryViewPager;
import info.emm.ui.Views.PZSImageView;
import info.emm.utils.ConstantValues;
import info.emm.utils.Utilities;
import info.emm.yuanchengcloudb.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class GalleryImageViewer extends AbstractGalleryActivity implements NotificationCenter.NotificationCenterDelegate {
    private TextView nameTextView;
    private TextView timeTextView;
    private View bottomView;
    private TextView fakeTitleView;
    private LocalPagerAdapter localPagerAdapter;
    private GalleryViewPager mViewPager;
    private boolean withoutBottom = false;
    private boolean fromAll = false;
    private boolean isVideo = false;
    private boolean needSearchMessage = false;
    private boolean loadingMore = false;
    private TextView title;
    private boolean ignoreSet = false;
    private ProgressBar loadingProgress;
    private String currentFileName;
    private int user_id = 0;
    private Point displaySize = new Point();
    private boolean cancelRunning = false;

    private ArrayList<MessageObject> imagesArrTemp = new ArrayList<MessageObject>();
    private HashMap<Integer, MessageObject> imagesByIdsTemp = new HashMap<Integer, MessageObject>();

    private long currentDialog = 0;
    private int totalCount = 0;
    private int classGuid;
    private boolean firstLoad = true;
    private boolean cacheEndReached = false;

    public static int needShowAllMedia = 2000;

    @SuppressLint("NewApi")
	@SuppressWarnings("unchecked")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Display display = getWindowManager().getDefaultDisplay();
        if(android.os.Build.VERSION.SDK_INT < 13) {
            displaySize.set(display.getWidth(), display.getHeight());
        } else {
            display.getSize(displaySize);
        }

        //xueqiang delete
        //classGuid = ConnectionsManager.getInstance().generateClassGuid();
        setContentView(R.layout.gallery_layout);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayShowHomeEnabled(ConstantValues.ActionBarShowLogo);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayUseLogoEnabled(true);
        actionBar.setTitle(getString(R.string.Gallery));
        actionBar.show();

        mViewPager = (GalleryViewPager)findViewById(R.id.gallery_view_pager);
        ImageView shareButton = (ImageView)findViewById(R.id.gallery_view_share_button);
        ImageView deleteButton = (ImageView) findViewById(R.id.gallery_view_delete_button);
        nameTextView = (TextView)findViewById(R.id.gallery_view_name_text);
        timeTextView = (TextView)findViewById(R.id.gallery_view_time_text);
        bottomView = findViewById(R.id.gallery_view_bottom_view);
        fakeTitleView = (TextView)findViewById(R.id.fake_title_view);
        loadingProgress = (ProgressBar)findViewById(R.id.action_progress);

        title = (TextView)findViewById(R.id.action_bar_title);
        if (title == null) {
            final int titleId = getResources().getIdentifier("action_bar_title", "id", "android");
            title = (TextView)findViewById(titleId);
        }

        NotificationCenter.getInstance().addObserver(this, FileLoader.FileDidFailedLoad);
        NotificationCenter.getInstance().addObserver(this, FileLoader.FileDidLoaded);
        NotificationCenter.getInstance().addObserver(this, FileLoader.FileLoadProgressChanged);
        NotificationCenter.getInstance().addObserver(this, MessagesController.mediaCountDidLoaded);
        NotificationCenter.getInstance().addObserver(this, MessagesController.mediaDidLoaded);
        NotificationCenter.getInstance().addObserver(this, MessagesController.userPhotosLoaded);
        NotificationCenter.getInstance().addObserver(this, 658);

        Integer index = null;
        if (localPagerAdapter == null) 
        {
        	//chatactivity
            final MessageObject file = (MessageObject)NotificationCenter.getInstance().getFromMemCache(51);
            //chatprofileactivity
            final TLRPC.FileLocation fileLocation = (TLRPC.FileLocation)NotificationCenter.getInstance().getFromMemCache(53);
            //mediaactivity
            final ArrayList<MessageObject> messagesArr = (ArrayList<MessageObject>)NotificationCenter.getInstance().getFromMemCache(54);
          //mediaactivity
            index = (Integer)NotificationCenter.getInstance().getFromMemCache(55);
            //settingactivity
            Integer uid = (Integer)NotificationCenter.getInstance().getFromMemCache(56);
            if (uid != null) {
                user_id = uid;
            }

            if (file != null) {
                ArrayList<MessageObject> imagesArr = new ArrayList<MessageObject>();
                HashMap<Integer, MessageObject> imagesByIds = new HashMap<Integer, MessageObject>();
                imagesArr.add(file);
                if (file.messageOwner.action == null || file.messageOwner.action instanceof TLRPC.TL_messageActionEmpty) {
                    needSearchMessage = true;
                    imagesByIds.put(file.messageOwner.id, file);
                    if (file.messageOwner.dialog_id != 0) {
                        currentDialog = file.messageOwner.dialog_id;
                    } else {
                        if (file.messageOwner.to_id.chat_id != 0) {
                            currentDialog = -file.messageOwner.to_id.chat_id;
                        } else {
                            if (file.messageOwner.to_id.user_id == UserConfig.clientUserId) {
                                currentDialog = file.messageOwner.from_id;
                            } else {
                                currentDialog = file.messageOwner.to_id.user_id;
                            }
                        }
                    }
                }
                localPagerAdapter = new LocalPagerAdapter(imagesArr, imagesByIds);
            } else if (fileLocation != null) {
                ArrayList<TLRPC.FileLocation> arr = new ArrayList<TLRPC.FileLocation>();
                arr.add(fileLocation);
                withoutBottom = true;
                deleteButton.setVisibility(View.INVISIBLE);
                nameTextView.setVisibility(View.INVISIBLE);
                timeTextView.setVisibility(View.INVISIBLE);
                localPagerAdapter = new LocalPagerAdapter(arr);
            } else if (messagesArr != null) {
                ArrayList<MessageObject> imagesArr = new ArrayList<MessageObject>();
                HashMap<Integer, MessageObject> imagesByIds = new HashMap<Integer, MessageObject>();
                imagesArr.addAll(messagesArr);
                Collections.reverse(imagesArr);
                for (MessageObject message : imagesArr) {
                    imagesByIds.put(message.messageOwner.id, message);
                }
                index = imagesArr.size() - index - 1;

                MessageObject object = imagesArr.get(0);
                if (object.messageOwner.dialog_id != 0) {
                    currentDialog = object.messageOwner.dialog_id;
                } else {
                    if (object.messageOwner.to_id.chat_id != 0) {
                        currentDialog = -object.messageOwner.to_id.chat_id;
                    } else {
                        if (object.messageOwner.to_id.user_id == UserConfig.clientUserId) {
                            currentDialog = object.messageOwner.from_id;
                        } else {
                            currentDialog = object.messageOwner.to_id.user_id;
                        }
                    }
                }
                localPagerAdapter = new LocalPagerAdapter(imagesArr, imagesByIds);
            }
        }

        mViewPager.setPageMargin(Utilities.dp(20));
        mViewPager.setOffscreenPageLimit(1);
        mViewPager.setAdapter(localPagerAdapter);

        if (index != null) {
            fromAll = true;
            mViewPager.setCurrentItem(index);
        }

        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    TLRPC.InputFileLocation file = getCurrentFile();
                    File f = new File(Utilities.getCacheDir(), currentFileName);
                    if (f.exists()) {
                        Intent intent = new Intent(Intent.ACTION_SEND);
                        if (file instanceof TLRPC.TL_inputVideoFileLocation) {
                            intent.setType("video/mp4");
                        } else {
                            intent.setType("image/jpeg");
                        }
                        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(f));
                        startActivity(intent);
                    }
                } catch (Exception e) {
                    FileLog.e("emm", e);
                }
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mViewPager == null || localPagerAdapter == null || localPagerAdapter.imagesArr == null) {
                    return;
                }
                int item = mViewPager.getCurrentItem();
                MessageObject obj = localPagerAdapter.imagesArr.get(item);
                if (obj.messageOwner.send_state == MessagesController.MESSAGE_SEND_STATE_SENT) {
                    ArrayList<Integer> arr = new ArrayList<Integer>();                    
                    arr.add(obj.messageOwner.id);
                  //xueqiang change
                    MessagesController.getInstance().deleteMessages(arr, null, null,true);
                    finish();
                }
            }
        });

        //����dialogͼƬ��db��,xueqiang change
        if (currentDialog != 0 && totalCount == 0) {
            MessagesController.getInstance().getMediaCount(currentDialog, classGuid, true);
        }
        if (user_id != 0) {
            MessagesController.getInstance().loadUserPhotos(user_id, 0, 30, 0, true, classGuid);
        }
        checkCurrentFile();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NotificationCenter.getInstance().removeObserver(this, FileLoader.FileDidFailedLoad);
        NotificationCenter.getInstance().removeObserver(this, FileLoader.FileDidLoaded);
        NotificationCenter.getInstance().removeObserver(this, FileLoader.FileLoadProgressChanged);
        NotificationCenter.getInstance().removeObserver(this, MessagesController.mediaCountDidLoaded);
        NotificationCenter.getInstance().removeObserver(this, MessagesController.mediaDidLoaded);
        NotificationCenter.getInstance().removeObserver(this, MessagesController.userPhotosLoaded);
        NotificationCenter.getInstance().removeObserver(this, 658);
        //xueqiang delete
        //ConnectionsManager.getInstance().cancelRpcsForClassGuid(classGuid);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void didReceivedNotification(int id, final Object... args) {
        if (id == FileLoader.FileDidFailedLoad) {
            String location = (String)args[0];
            if (currentFileName != null && currentFileName.equals(location)) {
                if (loadingProgress != null) {
                    loadingProgress.setVisibility(View.GONE);
                }
                if (localPagerAdapter != null) {
                    localPagerAdapter.updateViews();
                }
            }
        } else if (id == FileLoader.FileDidLoaded) {
            String location = (String)args[0];
            if (currentFileName != null && currentFileName.equals(location)) {
                if (loadingProgress != null) {
                    loadingProgress.setVisibility(View.GONE);
                }
                if (localPagerAdapter != null) {
                    localPagerAdapter.updateViews();
                }
            }
        } else if (id == FileLoader.FileLoadProgressChanged) {
            String location = (String)args[0];
            if (currentFileName != null && currentFileName.equals(location)) {
                Float progress = (Float)args[1];
                if (loadingProgress != null) {
                    loadingProgress.setVisibility(View.VISIBLE);
                    loadingProgress.setProgress((int)(progress * 100));
                }
                if (localPagerAdapter != null) {
                    localPagerAdapter.updateViews();
                }
            }
        } else if (id == MessagesController.userPhotosLoaded) {
            int guid = (Integer)args[4];
            int uid = (Integer)args[0];
            if (user_id == uid && classGuid == guid) {
                boolean fromCache = (Boolean)args[3];
                TLRPC.FileLocation currentLocation = null;
                int setToImage = -1;
                if (localPagerAdapter != null && mViewPager != null) {
                    int idx = mViewPager.getCurrentItem();
                    if (localPagerAdapter.imagesArrLocations.size() > idx) {
                        currentLocation = localPagerAdapter.imagesArrLocations.get(idx);
                    }
                }
                ArrayList<TLRPC.Photo> photos = (ArrayList<TLRPC.Photo>)args[5];
                if (photos.isEmpty()) {
                    return;
                }
                ArrayList<TLRPC.FileLocation> arr = new ArrayList<TLRPC.FileLocation>();
                for (TLRPC.Photo photo : photos) {
                    if (photo instanceof TLRPC.TL_photoEmpty) {
                        continue;

                    }
                    TLRPC.PhotoSize sizeFull = PhotoObject.getClosestPhotoSizeWithSize(photo.sizes, 800, 800);
                    if (sizeFull != null) {
                        if (currentLocation != null && sizeFull.location.local_id == currentLocation.local_id && sizeFull.location.volume_id == currentLocation.volume_id) {
                            setToImage = arr.size();
                        }
                        arr.add(sizeFull.location);
                    }
                }
                mViewPager.setAdapter(null);
                int count = mViewPager.getChildCount();
                for (int a = 0; a < count; a++) {
                    View child = mViewPager.getChildAt(0);
                    mViewPager.removeView(child);
                }
                mViewPager.mCurrentView = null;
                needSearchMessage = false;
                ignoreSet = true;
                mViewPager.setAdapter(localPagerAdapter = new LocalPagerAdapter(arr));
                mViewPager.invalidate();
                ignoreSet = false;
                if (setToImage != -1) {
                    mViewPager.setCurrentItem(setToImage);
                } else {
                    mViewPager.setCurrentItem(0);
                }
                if (fromCache) {
                    MessagesController.getInstance().loadUserPhotos(user_id, 0, 30, 0, false, classGuid);
                }
            }
        } else if (id == MessagesController.mediaCountDidLoaded) {
            long uid = (Long)args[0];
            if (uid == currentDialog) {
                if ((int)currentDialog != 0) {
                    boolean fromCache = (Boolean)args[2];
                    if (fromCache) {
                        MessagesController.getInstance().getMediaCount(currentDialog, classGuid, false);
                    }
                }
                totalCount = (Integer)args[1];
                if (needSearchMessage && firstLoad) {
                    firstLoad = false;
                    MessagesController.getInstance().loadMedia(currentDialog, 0, 100, 0, true, classGuid);
                    loadingMore = true;
                } else {
                    if (mViewPager != null && localPagerAdapter != null && localPagerAdapter.imagesArr != null) {
                        final int pos = (totalCount - localPagerAdapter.imagesArr.size()) + mViewPager.getCurrentItem() + 1;
                        Utilities.RunOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                                getSupportActionBar().setTitle(LocaleController.formatString("Of", R.string.Of, pos, totalCount));
                                if (title != null) {
                                    fakeTitleView.setText(LocaleController.formatString("Of", R.string.Of, pos, totalCount));
                                    fakeTitleView.measure(View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.AT_MOST), View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST));
                                    title.setWidth(fakeTitleView.getMeasuredWidth() + Utilities.dp(8));
                                    title.setMaxWidth(fakeTitleView.getMeasuredWidth() + Utilities.dp(8));
                                }
                            }
                        });
                    }
                }
            }
        } else if (id == MessagesController.mediaDidLoaded) {
            long uid = (Long)args[0];
            int guid = (Integer)args[4];
            if (uid == currentDialog && guid == classGuid) {
                if (localPagerAdapter == null || localPagerAdapter.imagesArr == null) {
                    return;
                }
                loadingMore = false;
                ArrayList<MessageObject> arr = (ArrayList<MessageObject>)args[2];
                boolean fromCache = (Boolean)args[3];
                cacheEndReached = !fromCache;
                if (needSearchMessage) {
                    if (arr.isEmpty()) {
                        needSearchMessage = false;
                        return;
                    }
                    int foundIndex = -1;

                    int index = mViewPager.getCurrentItem();
                    MessageObject currentMessage = localPagerAdapter.imagesArr.get(index);

                    int added = 0;
                    for (MessageObject message : arr) {
                        if (!imagesByIdsTemp.containsKey(message.messageOwner.id)) {
                            added++;
                            imagesArrTemp.add(0, message);
                            imagesByIdsTemp.put(message.messageOwner.id, message);
                            if (message.messageOwner.id == currentMessage.messageOwner.id) {
                                foundIndex = arr.size() - added;
                            }
                        }
                    }
                    if (added == 0) {
                        totalCount = imagesArrTemp.size();
                    }

                    if (foundIndex != -1) {
                        mViewPager.setAdapter(null);
                        int count = mViewPager.getChildCount();
                        for (int a = 0; a < count; a++) {
                            View child = mViewPager.getChildAt(0);
                            mViewPager.removeView(child);
                        }
                        mViewPager.mCurrentView = null;
                        needSearchMessage = false;
                        ignoreSet = true;
                        mViewPager.setAdapter(localPagerAdapter = new LocalPagerAdapter(imagesArrTemp, imagesByIdsTemp));
                        mViewPager.invalidate();
                        ignoreSet = false;
                        mViewPager.setCurrentItem(foundIndex);
                        imagesArrTemp = null;
                        imagesByIdsTemp = null;
                    } else {
                        if (!cacheEndReached || !arr.isEmpty()) {
                            MessageObject lastMessage = imagesArrTemp.get(0);
                            loadingMore = true;
                            MessagesController.getInstance().loadMedia(currentDialog, 0, 100, lastMessage.messageOwner.id, true, classGuid);
                        }
                    }
                } else {
                    int added = 0;
                    for (MessageObject message : arr) {
                        if (!localPagerAdapter.imagesByIds.containsKey(message.messageOwner.id)) {
                            added++;
                            localPagerAdapter.imagesArr.add(0, message);
                            localPagerAdapter.imagesByIds.put(message.messageOwner.id, message);
                        }
                    }
                    if (arr.isEmpty() && !fromCache) {
                        totalCount = arr.size();
                    }
                    if (added != 0) {
                        int current = mViewPager.getCurrentItem();
                        ignoreSet = true;
                        imagesArrTemp = new ArrayList<MessageObject>(localPagerAdapter.imagesArr);
                        imagesByIdsTemp = new HashMap<Integer, MessageObject>(localPagerAdapter.imagesByIds);
                        mViewPager.setAdapter(localPagerAdapter = new LocalPagerAdapter(imagesArrTemp, imagesByIdsTemp));
                        mViewPager.invalidate();
                        ignoreSet = false;
                        imagesArrTemp = null;
                        imagesByIdsTemp = null;
                        mViewPager.setCurrentItem(current + added);
                    } else {
                        totalCount = localPagerAdapter.imagesArr.size();
                    }
                }
            }
        } else if (id == 658) {
            try {
                if (!isFinishing()) {
                    finish();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private TLRPC.InputFileLocation getCurrentFile() {
        if (mViewPager == null || localPagerAdapter == null) {
            return null;
        }
        int item = mViewPager.getCurrentItem();
        if (withoutBottom) {
            TLRPC.FileLocation sizeFull = localPagerAdapter.imagesArrLocations.get(item);
            TLRPC.TL_inputFileLocation location = new TLRPC.TL_inputFileLocation();
            location.local_id = sizeFull.local_id;
            location.volume_id = sizeFull.volume_id;
            location.id = sizeFull.dc_id;
            location.secret = sizeFull.secret;
            return location;
        } else {
            MessageObject message = localPagerAdapter.imagesArr.get(item);
            if (message.messageOwner instanceof TLRPC.TL_messageService) {
                if (message.messageOwner.action instanceof TLRPC.TL_messageActionUserUpdatedPhoto) {
                    TLRPC.FileLocation sizeFull = message.messageOwner.action.newUserPhoto.photo_big;
                    TLRPC.TL_inputFileLocation location = new TLRPC.TL_inputFileLocation();
                    location.local_id = sizeFull.local_id;
                    location.volume_id = sizeFull.volume_id;
                    location.id = sizeFull.dc_id;
                    location.secret = sizeFull.secret;
                    return location;
                } else {
                    TLRPC.PhotoSize sizeFull = PhotoObject.getClosestPhotoSizeWithSize(message.messageOwner.action.photo.sizes, 800, 800);
                    if (sizeFull != null) {
                        TLRPC.TL_inputFileLocation location = new TLRPC.TL_inputFileLocation();
                        location.local_id = sizeFull.location.local_id;
                        location.volume_id = sizeFull.location.volume_id;
                        location.id = sizeFull.location.dc_id;
                        location.secret = sizeFull.location.secret;
                        return location;
                    }
                }
            } else if (message.messageOwner.media instanceof TLRPC.TL_messageMediaPhoto) {
                int width = (int)(Math.min(displaySize.x, displaySize.y) * 0.7f);
                int height = width + Utilities.dp(100);
                if (width > 800) {
                    width = 800;
                }
                if (height > 800) {
                    height = 800;
                }

                TLRPC.PhotoSize sizeFull = PhotoObject.getClosestPhotoSizeWithSize(message.messageOwner.media.photo.sizes, width, height);
                if (sizeFull != null) {
                    TLRPC.TL_inputFileLocation location = new TLRPC.TL_inputFileLocation();
                    location.local_id = sizeFull.location.local_id;
                    location.volume_id = sizeFull.location.volume_id;
                    location.id = sizeFull.location.dc_id;
                    location.secret = sizeFull.location.secret;
                    return location;
                }
            } else if (message.messageOwner.media instanceof TLRPC.TL_messageMediaVideo) {
                TLRPC.TL_inputVideoFileLocation location = new TLRPC.TL_inputVideoFileLocation();
                location.volume_id = message.messageOwner.media.video.dc_id;
                location.id = message.messageOwner.media.video.id;
                return location;
            }
        }
        return null;
    }

    @Override
    public void topBtn() {
        if (getSupportActionBar().isShowing()) {
            getSupportActionBar().hide();
            startViewAnimation(bottomView, false);
        } else {
            bottomView.setVisibility(View.VISIBLE);
            getSupportActionBar().show();
            startViewAnimation(bottomView, true);
        }
    }

    @Override
    public void didShowMessageObject(MessageObject obj) {
        TLRPC.User user = MessagesController.getInstance().users.get(obj.messageOwner.from_id);
        if (user != null) {
        	String nameString = Utilities.formatName(user);
            nameTextView.setText(nameString);
            timeTextView.setText(LocaleController.formatterYearMax.format(((long)obj.messageOwner.date) * 1000));
        } else {
            nameTextView.setText("");
        }
        isVideo = obj.messageOwner.media != null && obj.messageOwner.media instanceof TLRPC.TL_messageMediaVideo;

        if (obj.messageOwner instanceof TLRPC.TL_messageService) {
            if (obj.messageOwner.action instanceof TLRPC.TL_messageActionUserUpdatedPhoto) {
                TLRPC.FileLocation file = obj.messageOwner.action.newUserPhoto.photo_big;
                currentFileName = file.volume_id + "_" + file.local_id + ".jpg";
            } else {
                ArrayList<TLRPC.PhotoSize> sizes = obj.messageOwner.action.photo.sizes;
                if (sizes.size() > 0) {
                    int width = (int)(Math.min(displaySize.x, displaySize.y) * 0.7f);
                    int height = width + Utilities.dp(100);
                    if (width > 800) {
                        width = 800;
                    }
                    if (height > 800) {
                        height = 800;
                    }

                    TLRPC.PhotoSize sizeFull = PhotoObject.getClosestPhotoSizeWithSize(sizes, width, height);
                    if (sizeFull != null) {
                        currentFileName = sizeFull.location.volume_id + "_" + sizeFull.location.local_id + ".jpg";
                    }
                }
            }
        } else if (obj.messageOwner.media != null) {
            TLRPC.InputFileLocation file = getCurrentFile();
            if (file != null) {
                if (obj.messageOwner.media instanceof TLRPC.TL_messageMediaVideo) {
                    currentFileName = file.volume_id + "_" + file.id + ".mp4";
                } else if (obj.messageOwner.media instanceof TLRPC.TL_messageMediaPhoto) {
                    currentFileName = file.volume_id + "_" + file.local_id + ".jpg";
                }
            } else {
                currentFileName = null;
            }
        } else {
            currentFileName = null;
        }

        checkCurrentFile();
        supportInvalidateOptionsMenu();
    }

    private void checkCurrentFile() {
        if (currentFileName != null) {
            File f = new File(Utilities.getCacheDir(), currentFileName);
            if (f.exists()) {
                loadingProgress.setVisibility(View.GONE);
            } else {
                loadingProgress.setVisibility(View.VISIBLE);
                Float progress = FileLoader.getInstance().fileProgresses.get(currentFileName);
                if (progress != null) {
                    loadingProgress.setProgress((int)(progress * 100));
                } else {
                    loadingProgress.setProgress(0);
                }
            }
        } else {
            loadingProgress.setVisibility(View.GONE);
        }
        if (isVideo) {
            if (!FileLoader.getInstance().isLoadingFile(currentFileName)) {
                loadingProgress.setVisibility(View.GONE);
            } else {
                loadingProgress.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        if (withoutBottom) {
            inflater.inflate(R.menu.gallery_save_only_menu, menu);
        } else {
            inflater.inflate(R.menu.gallery_menu, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        if (currentFileName != null) {
            File f = new File(Utilities.getCacheDir(), currentFileName);
            if (f.exists()) {
                return super.onMenuOpened(featureId, menu);
            }
        }
        try {
            closeOptionsMenu();
        } catch (Exception e) {
            FileLog.e("emm", e);
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        processSelectedMenu(itemId);
        return true;
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        fixLayout();
    }

    private void fixLayout() {
        if (mViewPager != null) {
            ViewTreeObserver obs = mViewPager.getViewTreeObserver();
            obs.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    try {
                        mViewPager.beginFakeDrag();
                        if (mViewPager.isFakeDragging()) {
                            mViewPager.fakeDragBy(1);
                            mViewPager.endFakeDrag();
                        }
                    } catch (Exception e) {
                        FileLog.e("emm", e);
                    }
                    mViewPager.getViewTreeObserver().removeOnPreDrawListener(this);
                    return false;
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        cancelRunning = true;
        mViewPager.setAdapter(null);
        localPagerAdapter = null;
        finish();
        System.gc();
    }

    private void processSelectedMenu(int itemId) {
        if (itemId == android.R.id.home) {
            cancelRunning = true;
            if (mViewPager != null) {
                mViewPager.setAdapter(null);
            }
            localPagerAdapter = null;
            finish();
            System.gc();

        } else if (itemId == R.id.gallery_menu_save) {
            if (currentFileName == null) {
                return;
            }
            if (isVideo) {
                MediaController.saveFile(currentFileName, this, 1, null);
            } else {
                MediaController.saveFile(currentFileName, this, 0, null);
            }

//            case R.id.gallery_menu_send: {
//                Intent intent = new Intent(this, MessagesActivity.class);
//                intent.putExtra("onlySelect", true);
//                startActivityForResult(intent, 10);
//                break;
//            }
        } else if (itemId == R.id.gallery_menu_showall) {
            if (fromAll) {
                finish();
            } else {
                if (localPagerAdapter != null && localPagerAdapter.imagesArr != null && !localPagerAdapter.imagesArr.isEmpty() && currentDialog != 0) {
                    finish();
                    NotificationCenter.getInstance().postNotificationName(needShowAllMedia, currentDialog);
                }
            }
        }
    }

    /*@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == 10) {
                int chatId = data.getIntExtra("chatId", 0);
                int userId = data.getIntExtra("userId", 0);
                int dialog_id = 0;
                if (chatId != 0) {
                    dialog_id = -chatId;
                } else if (userId != 0) {
                    dialog_id = userId;
                }
                TLRPC.FileLocation location = getCurrentFile();
                if (dialog_id != 0 && location != null) {
                    Intent intent = new Intent(GalleryImageViewer.this, ChatActivity.class);
                    if (chatId != 0) {
                        intent.putExtra("chatId", chatId);
                    } else {
                        intent.putExtra("userId", userId);
                    }
                    startActivity(intent);
                    NotificationCenter.getInstance().postNotificationName(MessagesController.closeChats);
                    finish();
                    if (withoutBottom) {
                        MessagesController.getInstance().sendMessage(location, dialog_id);
                    } else {
                        int item = mViewPager.getCurrentItem();
                        MessageObject obj = localPagerAdapter.imagesArr.get(item);
                        MessagesController.getInstance().sendMessage(obj, dialog_id);
                    }
                }
            }
        }
    }*/

    private void startViewAnimation(final View panel, boolean up) {
        Animation animation;
        if (!up) {
            animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                    Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 1.0f);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    panel.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            animation.setDuration(400);
        } else {
            animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                    Animation.RELATIVE_TO_SELF, 1.0f, Animation.RELATIVE_TO_SELF, 0.0f);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    panel.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animation animation) {

                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            animation.setDuration(100);
        }
        panel.startAnimation(animation);
    }

    public class LocalPagerAdapter extends PagerAdapter {
        public ArrayList<MessageObject> imagesArr;
        public HashMap<Integer, MessageObject> imagesByIds;
        private ArrayList<TLRPC.FileLocation> imagesArrLocations;

        public LocalPagerAdapter(ArrayList<MessageObject> _imagesArr, HashMap<Integer, MessageObject> _imagesByIds) {
            imagesArr = _imagesArr;
            imagesByIds = _imagesByIds;
        }

        public LocalPagerAdapter(ArrayList<TLRPC.FileLocation> locations) {
            imagesArrLocations = locations;
        }

        @Override
        public void setPrimaryItem(ViewGroup container, final int position, Object object) {
            super.setPrimaryItem(container, position, object);
            if (container == null || object == null || ignoreSet) {
                return;
            }
            ((GalleryViewPager) container).mCurrentView = (PZSImageView)((View) object).findViewById(R.id.page_image);
            if (imagesArr != null) {
                didShowMessageObject(imagesArr.get(position));
                final int imageArrSize = imagesArr.size();
                totalCount = imageArrSize;
                if (totalCount != 0 && !needSearchMessage) {
                    if (imageArrSize < totalCount && !loadingMore && position < 5) {
                        MessageObject lastMessage = imagesArr.get(0);
                        MessagesController.getInstance().loadMedia(currentDialog, 0, 100, lastMessage.messageOwner.id, !cacheEndReached, classGuid);
                        loadingMore = true;
                    }


                    Utilities.RunOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            getSupportActionBar().setTitle(LocaleController.formatString("Of", R.string.Of, (totalCount - imageArrSize) + position + 1, totalCount));
                            if (title != null) {
                                fakeTitleView.setText(LocaleController.formatString("Of", R.string.Of, (totalCount - imageArrSize) + position + 1, totalCount));
                                fakeTitleView.measure(View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.AT_MOST), View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST));
                                title.setWidth(fakeTitleView.getMeasuredWidth() + Utilities.dp(8));
                                title.setMaxWidth(fakeTitleView.getMeasuredWidth() + Utilities.dp(8));
                            }
                        }
                    });

                }
            } else if (imagesArrLocations != null) {
                TLRPC.FileLocation file = imagesArrLocations.get(position);
                currentFileName = file.volume_id + "_" + file.local_id + ".jpg";
                checkCurrentFile();
               final int imageSize = imagesArrLocations.size();
               totalCount = imageSize;
                if (imageSize > 1) {
                    Utilities.RunOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            getSupportActionBar().setTitle(LocaleController.formatString("Of", R.string.Of, position + 1, imageSize));
                            if (title != null) {
                                fakeTitleView.setText(LocaleController.formatString("Of", R.string.Of, position + 1, imageSize));
                                fakeTitleView.measure(View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.AT_MOST), View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST));
                                title.setWidth(fakeTitleView.getMeasuredWidth() + Utilities.dp(8));
                                title.setMaxWidth(fakeTitleView.getMeasuredWidth() + Utilities.dp(8));
                            }
                        }
                    });
                } else {
                    getSupportActionBar().setTitle(getString(R.string.Gallery));
                }
            }
        }

        public void updateViews() {
            int count = mViewPager.getChildCount();
            for (int a = 0; a < count; a++) {
                View v = mViewPager.getChildAt(a);
                final TextView playButton = (TextView)v.findViewById(R.id.action_button);
                MessageObject message = (MessageObject)playButton.getTag();
                if (message != null) {
                    processViews(playButton, message);
                }
            }
        }

        public void processViews(TextView playButton, MessageObject message) {
            if (message.messageOwner.send_state != MessagesController.MESSAGE_SEND_STATE_SENDING && message.messageOwner.send_state != MessagesController.MESSAGE_SEND_STATE_SEND_ERROR) {
                playButton.setVisibility(View.VISIBLE);
                String fileName = message.messageOwner.media.video.dc_id + "_" + message.messageOwner.media.video.id + ".mp4";
                boolean load = false;
                if (message.messageOwner.attachPath != null && message.messageOwner.attachPath.length() != 0) {
                    File f = new File(message.messageOwner.attachPath);
                    if (f.exists()) {
                        playButton.setText(getString(R.string.ViewVideo));
                    } else {
                        load = true;
                    }
                } else {
                    File cacheFile = new File(Utilities.getCacheDir(), fileName);
                    if (cacheFile.exists()) {
                        playButton.setText(getString(R.string.ViewVideo));
                    } else {
                        load = true;
                    }
                }
                if (load) {
                    Float progress = FileLoader.getInstance().fileProgresses.get(fileName);
                    if (FileLoader.getInstance().isLoadingFile(fileName)) {
                        playButton.setText(LocaleController.getString("CancelDownload", R.string.CancelDownload));
                    } else {
                        playButton.setText(String.format("%s %.1f MB", LocaleController.getString("DOWNLOAD", R.string.DOWNLOAD), message.messageOwner.media.video.size / 1024.0f / 1024.0f));
                    }
                }
            }
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public Object instantiateItem(View collection, int position) {
            View view = View.inflate(collection.getContext(), R.layout.gallery_page_layout, null);
            ((ViewPager) collection).addView(view, 0);

            PZSImageView iv = (PZSImageView)view.findViewById(R.id.page_image);
            final TextView playButton = (TextView)view.findViewById(R.id.action_button);

            if (imagesArr != null) {
                final MessageObject message = imagesArr.get(position);
                view.setTag(message.messageOwner.id);
                if (message.messageOwner instanceof TLRPC.TL_messageService) {
                    if (message.messageOwner.action instanceof TLRPC.TL_messageActionUserUpdatedPhoto) {
                        iv.isVideo = false;
                        iv.setImage(message.messageOwner.action.newUserPhoto.photo_big, null, 0, -1);
                    } else {
                        ArrayList<TLRPC.PhotoSize> sizes = message.messageOwner.action.photo.sizes;
                        iv.isVideo = false;
                        if (sizes.size() > 0) {
                            TLRPC.PhotoSize sizeFull = PhotoObject.getClosestPhotoSizeWithSize(sizes, 800, 800);
                            if (message.imagePreview != null) {
                                iv.setImage(sizeFull.location, null, message.imagePreview, sizeFull.size);
                            } else {
                                iv.setImage(sizeFull.location, null, 0, sizeFull.size);
                            }
                        }
                    }
                } else if (message.messageOwner.media instanceof TLRPC.TL_messageMediaPhoto) {
                    ArrayList<TLRPC.PhotoSize> sizes = message.messageOwner.media.photo.sizes;
                    iv.isVideo = false;
                    if (sizes.size() > 0) {
                        int width = (int)(Math.min(displaySize.x, displaySize.y) * 0.7f);
                        int height = width + Utilities.dp(100);
                        if (width > 800) {
                            width = 800;
                        }
                        if (height > 800) {
                            height = 800;
                        }

                        TLRPC.PhotoSize sizeFull = PhotoObject.getClosestPhotoSizeWithSize(sizes, width, height);
                        if (message.imagePreview != null) {
                            iv.setImage(sizeFull.location, null, message.imagePreview, sizeFull.size);
                        } else {
                            iv.setImage(sizeFull.location, null, 0, sizeFull.size);
                        }
                    }
                } else if (message.messageOwner.media instanceof TLRPC.TL_messageMediaVideo) {
                    processViews(playButton, message);
                    playButton.setTag(message);

                    playButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            boolean loadFile = false;
                            String fileName = message.messageOwner.media.video.dc_id + "_" + message.messageOwner.media.video.id + ".mp4";
                            if (message.messageOwner.attachPath != null && message.messageOwner.attachPath.length() != 0) {
                                File f = new File(message.messageOwner.attachPath);
                                if (f.exists()) {
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    intent.setDataAndType(Uri.fromFile(f), "video/mp4");
                                    startActivity(intent);
                                } else {
                                    loadFile = true;
                                }
                            } else {
                                File cacheFile = new File(Utilities.getCacheDir(), fileName);
                                if (cacheFile.exists()) {
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    intent.setDataAndType(Uri.fromFile(cacheFile), "video/mp4");
                                    startActivity(intent);
                                } else {
                                    loadFile = true;
                                }
                            }
                            if (loadFile) {
                                if (!FileLoader.getInstance().isLoadingFile(fileName)) {
                                    FileLoader.getInstance().loadFile(message.messageOwner.media.video, null, null, null);
                                } else {
                                    FileLoader.getInstance().cancelLoadFile(message.messageOwner.media.video, null, null, null);
                                }
                                checkCurrentFile();
                                processViews(playButton, message);
                            }
                        }
                    });
                    iv.isVideo = true;
                    if (message.messageOwner.media.video.thumb instanceof TLRPC.TL_photoCachedSize) {
                        iv.setImageBitmap(message.imagePreview);
                    } else {
                        if (message.messageOwner.media.video.thumb != null) {
                            iv.setImage(message.messageOwner.media.video.thumb.location, null, 0, message.messageOwner.media.video.thumb.size);
                        }
                    }
                }
            } else {
                iv.isVideo = false;
                iv.setImage(imagesArrLocations.get(position), null, 0, -1);
            }

            return view;
        }

        @Override
        public void destroyItem(View collection, int position, Object view) {
            ((ViewPager)collection).removeView((View)view);
            PZSImageView iv = (PZSImageView)((View)view).findViewById(R.id.page_image);
            if (cancelRunning) {
                FileLoader.getInstance().cancelLoadingForImageView(iv);
            }
            iv.clearImage();
        }

        @Override
        public int getCount() 
        {
            if (imagesArr != null) 
            {
//            	FileLog.e("emm", "imagearray="+imagesArr.size());
                return imagesArr.size();
            } else if (imagesArrLocations != null) {
                return imagesArrLocations.size();
            } else {
                return 0;
            }
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Parcelable saveState() {
            return null;
        }

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {

        }

        @Override
        public void finishUpdate(View container) {

        }

        @Override
        public void startUpdate(View container) {

        }
    }
}
