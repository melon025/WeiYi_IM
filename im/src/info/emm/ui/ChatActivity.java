/*
 * This is the source code of Emm for Android v. 1.3.2.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013.
 */

package info.emm.ui;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.v4.internal.view.SupportMenuItem;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import info.emm.LocalData.Config;
import info.emm.LocalData.DateUnit;
import info.emm.PhoneFormat.PhoneFormat;
import info.emm.im.meeting.MeetingMgr;
import info.emm.messenger.ConnectionsManager;
import info.emm.messenger.ContactsController;
import info.emm.messenger.Emoji;
import info.emm.messenger.FileLoader;
import info.emm.messenger.FileLog;
import info.emm.messenger.LocaleController;
import info.emm.messenger.MediaController;
import info.emm.messenger.MessagesController;
import info.emm.messenger.MessagesStorage;
import info.emm.messenger.NotificationCenter;
import info.emm.messenger.ProximitySensorAware;
import info.emm.messenger.ProximitySensorManager;
import info.emm.messenger.IMRtmpClientMgr;
import info.emm.messenger.TLRPC;
import info.emm.messenger.UserConfig;
import info.emm.objects.MessageObject;
import info.emm.objects.PhotoObject;
import info.emm.services.UEngine;
import info.emm.ui.Cells.ChatAudioCell;
import info.emm.ui.Cells.ChatBaseCell;
import info.emm.ui.Cells.ChatMessageCell;
import info.emm.ui.Cells.ChatReminderCell;
import info.emm.ui.Views.BackupImageView;
import info.emm.ui.Views.BaseFragment;
import info.emm.ui.Views.EmojiView;
import info.emm.ui.Views.LayoutListView;
import info.emm.ui.Views.MessageActionLayout;
import info.emm.ui.Views.OnSwipeTouchListener;
import info.emm.ui.Views.SizeNotifierRelativeLayout;
import info.emm.ui.Views.TimerButton;
import info.emm.utils.StringUtil;
import info.emm.utils.UiUtil;
import info.emm.utils.Utilities;
import info.emm.yuanchengcloudb.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;


//import com.google.android.gms.common.ConnectionResult;
//import com.google.android.gms.common.GooglePlayServicesUtil;
//xueqiang add for voice  Recognize

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;
import com.utils.Utitlties;
import com.utils.WeiyiMeeting;
import com.utils.WeiyiMeetingNotificationCenter;


/**
 * 锟斤拷锟斤拷锟斤拷锟�
 *
 * @author Administrator
 */

@SuppressLint("NewApi")
public class ChatActivity extends BaseFragment implements
        SizeNotifierRelativeLayout.SizeNotifierRelativeLayoutDelegate,
        NotificationCenter.NotificationCenterDelegate,
        MessagesActivity.MessagesActivityDelegate,
        DocumentSelectActivity.DocumentSelectActivityDelegate
        , ProximitySensorAware, WeiyiMeetingNotificationCenter.NotificationCenterDelegate {
    private LayoutListView chatListView;

    private BackupImageView avatarImageView;

    private TLRPC.Chat currentChat;

    private TLRPC.User currentUser;

    private TLRPC.EncryptedChat currentEncryptedChat;

    private ChatAdapter chatAdapter;

    private EditText messsageEditText;

    private ImageButton sendButton;

    private PopupWindow emojiPopup;

    private ImageView emojiButton;

    private EmojiView emojiView;//锟斤拷锟斤拷

    private View slideText;

    private boolean keyboardVisible;

    private int keyboardHeight = 0;

    private int keyboardHeightLand = 0;

    private View topPanel;

    private View secretChatPlaceholder;

    private View contentView;

    private View progressView;

    private boolean ignoreTextChange = false;

    private TextView emptyView;

    private View bottomOverlay;

    private View recordPanel;

    private TextView recordTimeText;

    private TextView bottomOverlayText;

    private ImageButton audioSendButton;

    private MessageObject selectedObject;

    private MessageObject forwaringMessage;

    private TextView secretViewStatusTextView;

    private TimerButton timerButton;

    private LinearLayout callLayout;
    private TextView callInfo;

    private Point displaySize = new Point();

    private boolean paused = true;

    private boolean readWhenResume = false;

    private boolean sendByEnter = false;

    private int readWithDate = 0;

    private int readWithMid = 0;

    private boolean swipeOpening = false;

    public boolean scrollToTopOnResume = false;

    private boolean scrollToTopUnReadOnResume = false;

    private boolean isCustomTheme = false;

    private boolean downloadPhotos = true;

    private boolean downloadAudios = true;

    private ImageView topPlaneClose;

    private View pagedownButton;

    private TextView topPanelText;

    private long dialog_id;

    private int mCompanyID = -1;

    private int mDeptID = -1;

    private String locale_language;

    AlertDialog visibleDialog = null;

    private SizeNotifierRelativeLayout sizeNotifierRelativeLayout;

    private HashMap<Integer, MessageObject> selectedMessagesIds = new HashMap<Integer, MessageObject>();//删锟斤拷

    private HashMap<Integer, MessageObject> selectedMessagesCanCopyIds = new HashMap<Integer, MessageObject>();//锟斤拷锟斤拷

    private HashMap<Integer, MessageObject> selectedMessagesCanNotForwardIds = new HashMap<Integer, MessageObject>();

    private HashMap<Integer, MessageObject> messagesDict = new HashMap<Integer, MessageObject>();

    private HashMap<String, ArrayList<MessageObject>> messagesByDays = new HashMap<String, ArrayList<MessageObject>>();

    private ArrayList<MessageObject> messages = new ArrayList<MessageObject>();

    private int maxMessageId = Integer.MAX_VALUE;

    private int minMessageId = Integer.MIN_VALUE;

    private int maxDate = Integer.MIN_VALUE;

    private boolean endReached = false;

    private boolean loading = false;

    private boolean cacheEndReaced = false;

    private long lastTypingTimeSend = 0;

    private int minDate = 0;

    private int progressTag = 0;

    private boolean invalidateAfterAnimation = false;

    boolean first = true;

    private int unread_to_load = 0;

    private int first_unread_id = 0;

    private int last_unread_id = 0;

    private boolean unread_end_reached = true;

    private boolean loadingForward = false;

    private MessageObject unreadMessageObject = null;

    private boolean recordingAudio = false;

    private String lastTimeString = null;

    private float startedDraggingX = -1;

    private float distCanMove = Utilities.dp(80);

    private PowerManager.WakeLock mWakeLock = null;

    private int prevOrientation = -10;

    private String currentPicturePath;

    private TLRPC.ChatParticipants chatinfo = null;

    private int onlineCount = -1;

    private HashMap<String, ProgressBar> progressBarMap = new HashMap<String, ProgressBar>();

    private HashMap<String, ArrayList<ProgressBar>> loadingFile = new HashMap<String, ArrayList<ProgressBar>>();

    private HashMap<Integer, String> progressByTag = new HashMap<Integer, String>();

    //true表示显示呼叫button,false表示不显示
    private boolean toCallLimit = true;
    // xueqiang change for 锟斤拷锟斤拷锟斤拷频锟斤拷锟斤拷
    // private ArrayList<ChatAudioCell> AudioCellArrayList = new
    // ArrayList<ChatAudioCell>();

    // sam
    // sam
    /**
     * The time to wait before enabling the blank the screen due to the
     * proximity sensor.
     */
    private static final long PROXIMITY_BLANK_DELAY_MILLIS = 100;

    /**
     * The time to wait before disabling the blank the screen due to the
     * proximity sensor.
     */
    private static final long PROXIMITY_UNBLANK_DELAY_MILLIS = 1000;

    private ProximitySensorManager mProximitySensorManager;

    private final ProximitySensorListener mProximitySensorListener = new ProximitySensorListener();

    private boolean isNear = false;

    private CharSequence lastPrintString;

    ActionMode mActionMode = null;

    private View bottomOverlayChat;

    private Handler handler = new Handler(Looper.getMainLooper());


    //only for voice reganize
    private static String TAG = "IatDemo";
    // 语音听写对象
    private SpeechRecognizer mIat;
    // 听写结果内容
    private String mResultText = "";
    // 用户词表下载结果
    private Toast mToast;
    // 语音听写UI
    private RecognizerDialog iatDialog;


    private MessageObject selectMessage;

    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            menu.clear();
            MenuInflater inflater = actionMode.getMenuInflater();
            if (currentEncryptedChat == null) {
                inflater.inflate(R.menu.messages_full_menu, menu);//复制  转发  删除
            } else {
                inflater.inflate(R.menu.messages_encrypted_menu, menu);//复制  删除
            }
            menu.findItem(R.id.copy).setVisible(
                    selectedMessagesCanCopyIds.size() != 0);
            if (menu.findItem(R.id.forward) != null) {
                menu.findItem(R.id.forward).setVisible(
                        selectedMessagesCanNotForwardIds.size() == 0);
            }

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @SuppressLint("NewApi")
        @Override
        public boolean onActionItemClicked(ActionMode actionMode,
                                           MenuItem menuItem) {
            int i = menuItem.getItemId();
            if (i == R.id.copy) {
                String str = "";
                ArrayList<Integer> ids = new ArrayList<Integer>(
                        selectedMessagesCanCopyIds.keySet());
                if (currentEncryptedChat == null) {
                    Collections.sort(ids);
                } else {
                    Collections.sort(ids, Collections.reverseOrder());
                }
                for (Integer id : ids) {
                    MessageObject messageObject = selectedMessagesCanCopyIds
                            .get(id);
                    if (str.length() != 0) {
                        str += "\n";
                    }
                    str += messageObject.messageOwner.message;
                }
                if (str.length() != 0) {
                    if (android.os.Build.VERSION.SDK_INT < 11) {
                        android.text.ClipboardManager clipboard = (android.text.ClipboardManager) parentActivity
                                .getSystemService(Context.CLIPBOARD_SERVICE);
                        clipboard.setText(str);
                    } else {
                        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) parentActivity
                                .getSystemService(Context.CLIPBOARD_SERVICE);
                        android.content.ClipData clip = android.content.ClipData
                                .newPlainText("label", str);
                        clipboard.setPrimaryClip(clip);
                    }
                }
            } else if (i == R.id.delete) {
                ArrayList<Integer> ids = new ArrayList<Integer>(
                        selectedMessagesIds.keySet());
                ArrayList<Long> random_ids = null;
                if (currentEncryptedChat != null) {
                    random_ids = new ArrayList<Long>();
                    for (HashMap.Entry<Integer, MessageObject> entry : selectedMessagesIds
                            .entrySet()) {
                        MessageObject msg = entry.getValue();
                        if (msg.messageOwner.random_id != 0) {
                            random_ids.add(msg.messageOwner.random_id);
                        }
                    }
                }
                MessagesController.getInstance().deleteMessages(ids,
                        random_ids, currentEncryptedChat, true);
            } else if (i == R.id.forward) {
                MessagesActivity fragment = new MessagesActivity();//转发到自己创建的群fragment.selectAlertString = R.string.ForwardMessagesTo;fragment.selectAlertStringDesc = "ForwardMessagesTo";fragment.animationType = 1;Bundle args = new Bundle();args.putBoolean("onlySelect", true);args.putBoolean("serverOnly", true);args.putString("typeFace", "retransmit");fragment.setArguments(args);fragment.delegate = ChatActivity.this;((LaunchActivity) parentActivity).presentFragment(fragment, "select_chat", false);
            }
            actionMode.finish();
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            mActionMode = null;
            updateVisibleRows();
        }
    };

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        int chatId = getArguments().getInt("chat_id", 0);
        int userId = getArguments().getInt("user_id", 0);
        int encId = getArguments().getInt("enc_id", 0);
        locale_language = Locale.getDefault().getCountry();
        mCompanyID = getArguments().getInt(Config.CompanyID, -1);
        mDeptID = getArguments().getInt(Config.DeptID, -1);

        if (chatId != 0) {
            currentChat = MessagesController.getInstance().chats.get(chatId);
            if (currentChat == null) {
                return false;
            }
            MessagesController.getInstance().loadChatInfo(currentChat.id);
            dialog_id = -chatId;
        } else if (userId != 0) {
            currentUser = MessagesController.getInstance().users.get(userId);
            if (currentUser == null) {
                return false;
            }
            dialog_id = userId;
        } else if (encId != 0) {
            currentEncryptedChat = MessagesController.getInstance().encryptedChats
                    .get(encId);
            if (currentEncryptedChat == null) {
                return false;
            }
            currentUser = MessagesController.getInstance().users
                    .get(currentEncryptedChat.user_id);
            if (currentUser == null) {
                return false;
            }
            dialog_id = ((long) encId) << 32;
            maxMessageId = Integer.MIN_VALUE;
            minMessageId = Integer.MAX_VALUE;
        } else {
            return false;
        }
        NotificationCenter.getInstance().addObserver(this,
                MessagesController.messagesDidLoaded);
        NotificationCenter.getInstance().addObserver(this, 999);
        NotificationCenter.getInstance().addObserver(this,
                MessagesController.updateInterfaces);
        NotificationCenter.getInstance().addObserver(this,
                MessagesController.didReceivedNewMessages);
        NotificationCenter.getInstance().addObserver(this,
                MessagesController.closeChats);
        NotificationCenter.getInstance().addObserver(this,
                MessagesController.messagesReaded);
        NotificationCenter.getInstance().addObserver(this,
                MessagesController.messagesDeleted);
        NotificationCenter.getInstance().addObserver(this,
                MessagesController.messageReceivedByServer);
        NotificationCenter.getInstance().addObserver(this,
                MessagesController.messageReceivedByAck);
        NotificationCenter.getInstance().addObserver(this,
                MessagesController.messageSendError);
        NotificationCenter.getInstance().addObserver(this,
                MessagesController.chatInfoDidLoaded);
        NotificationCenter.getInstance().addObserver(this,
                MessagesController.contactsDidLoaded);
        NotificationCenter.getInstance().addObserver(this,
                MessagesController.encryptedChatUpdated);
        NotificationCenter.getInstance().addObserver(this,
                MessagesController.messagesReadedEncrypted);
        NotificationCenter.getInstance().addObserver(this,
                MessagesController.removeAllMessagesFromDialog);
        NotificationCenter.getInstance().addObserver(this,
                FileLoader.FileUploadProgressChanged);
        NotificationCenter.getInstance().addObserver(this,
                FileLoader.FileDidFailedLoad);
        NotificationCenter.getInstance().addObserver(this,
                FileLoader.FileDidLoaded);
        NotificationCenter.getInstance().addObserver(this,
                FileLoader.FileLoadProgressChanged);
        NotificationCenter.getInstance().addObserver(this,
                MediaController.audioProgressDidChanged);
        NotificationCenter.getInstance().addObserver(this,
                MediaController.audioDidReset);
        NotificationCenter.getInstance().addObserver(this,
                MediaController.recordProgressChanged);
        NotificationCenter.getInstance().addObserver(this,
                MediaController.recordStarted);
        NotificationCenter.getInstance().addObserver(this,
                MediaController.recordStartError);
        NotificationCenter.getInstance().addObserver(this,
                MediaController.recordStopped);
        NotificationCenter.getInstance().addObserver(this,
                MediaController.startplayaudio);
        NotificationCenter.getInstance().addObserver(this,
                MediaController.playaudiocomplete);

        NotificationCenter.getInstance().addObserver(this, 997);
        NotificationCenter.getInstance().addObserver(this, MessagesController.meeting_notice_bar);
        NotificationCenter.getInstance().addObserver(this, MessagesController.alermDidLoaded);

        NotificationCenter.getInstance().addObserver(this, MeetingMgr.CHECK_MEETING);
        NotificationCenter.getInstance().addObserver(this, MessagesController.EnterMeeting_Complete);
        NotificationCenter.getInstance().addObserver(this, MessagesController.meeting_call_response);


        loading = true;
        FileLog.e("emm", "load messages begin from ui");
        MessagesController.getInstance().loadMessages(dialog_id, 0, 30, 0,
                true, 0, classGuid, true, false);
        SharedPreferences preferences = ApplicationLoader.applicationContext
                .getSharedPreferences("mainconfig_" + UserConfig.clientUserId,
                        Activity.MODE_PRIVATE);
        sendByEnter = preferences.getBoolean("send_by_enter", false);

        if (currentChat != null) {
            downloadPhotos = preferences
                    .getBoolean("photo_download_chat", true);
        } else {
            downloadPhotos = preferences
                    .getBoolean("photo_download_user", true);
        }
        if (currentChat != null) {
            downloadAudios = preferences
                    .getBoolean("audio_download_chat", true);
        } else {
            downloadAudios = preferences
                    .getBoolean("audio_download_user", true);
        }
        FileLog.e("emm", "chatactivity onFragmentCreate");
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getInstance().removeObserver(this,
                MessagesController.messagesDidLoaded);
        NotificationCenter.getInstance().removeObserver(this, 999);
        NotificationCenter.getInstance().removeObserver(this,
                MessagesController.updateInterfaces);
        NotificationCenter.getInstance().removeObserver(this,
                MessagesController.didReceivedNewMessages);
        NotificationCenter.getInstance().removeObserver(this,
                MessagesController.closeChats);
        NotificationCenter.getInstance().removeObserver(this,
                MessagesController.messagesReaded);
        NotificationCenter.getInstance().removeObserver(this,
                MessagesController.messagesDeleted);
        NotificationCenter.getInstance().removeObserver(this,
                MessagesController.messageReceivedByServer);
        NotificationCenter.getInstance().removeObserver(this,
                MessagesController.messageReceivedByAck);
        NotificationCenter.getInstance().removeObserver(this,
                MessagesController.messageSendError);
        NotificationCenter.getInstance().removeObserver(this,
                MessagesController.chatInfoDidLoaded);
        NotificationCenter.getInstance().removeObserver(this,
                MessagesController.encryptedChatUpdated);
        NotificationCenter.getInstance().removeObserver(this,
                MessagesController.messagesReadedEncrypted);
        NotificationCenter.getInstance().removeObserver(this,
                MessagesController.removeAllMessagesFromDialog);
        NotificationCenter.getInstance().removeObserver(this,
                FileLoader.FileUploadProgressChanged);
        NotificationCenter.getInstance().removeObserver(this,
                FileLoader.FileDidFailedLoad);
        NotificationCenter.getInstance().removeObserver(this,
                FileLoader.FileDidLoaded);
        NotificationCenter.getInstance().removeObserver(this,
                FileLoader.FileLoadProgressChanged);
        NotificationCenter.getInstance().removeObserver(this,
                MessagesController.contactsDidLoaded);
        NotificationCenter.getInstance().removeObserver(this,
                MediaController.audioProgressDidChanged);
        NotificationCenter.getInstance().removeObserver(this,
                MediaController.audioDidReset);
        NotificationCenter.getInstance().removeObserver(this,
                MediaController.recordProgressChanged);
        NotificationCenter.getInstance().removeObserver(this,
                MediaController.recordStarted);
        NotificationCenter.getInstance().removeObserver(this,
                MediaController.recordStartError);
        NotificationCenter.getInstance().removeObserver(this,
                MediaController.recordStopped);
        NotificationCenter.getInstance().removeObserver(this,
                MediaController.startplayaudio);
        NotificationCenter.getInstance().removeObserver(this,
                MediaController.playaudiocomplete);

        NotificationCenter.getInstance().removeObserver(this, 997);
        NotificationCenter.getInstance().removeObserver(this, MessagesController.meeting_notice_bar);
        NotificationCenter.getInstance().removeObserver(this, MessagesController.alermDidLoaded);

        //add by xueqiang
        NotificationCenter.getInstance().removeObserver(this, MeetingMgr.CHECK_MEETING);


        if (sizeNotifierRelativeLayout != null) {
            sizeNotifierRelativeLayout.delegate = null;
            sizeNotifierRelativeLayout = null;
        }

        try {
            if (visibleDialog != null) {
                visibleDialog.dismiss();
                visibleDialog = null;
            }
        } catch (Exception e) {
            FileLog.e("emm", e);
        }
        if (mWakeLock != null) {
            try {
                mWakeLock.release();
                mWakeLock = null;
            } catch (Exception e) {
                FileLog.e("emm", e);
            }
        }
        try {
            if (prevOrientation != -10) {
                parentActivity.setRequestedOrientation(prevOrientation);
                prevOrientation = -10;
            }
        } catch (Exception e) {
            FileLog.e("emm", e);
        }
        MediaController.getInstance().stopAudio();

        FileLog.e("emm", "chatactivity onFragmentDestory");
    }

    @SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        FileLog.d("emm", "chat onCreate");
        super.onCreate(savedInstanceState);
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        setHasOptionsMenu(true);
        FileLog.e("emm", "chat oncreate**************************");
        Display display = parentActivity.getWindowManager().getDefaultDisplay();
        if (android.os.Build.VERSION.SDK_INT < 13) {
            displaySize.set(display.getWidth(), display.getHeight());
        } else {
            display.getSize(displaySize);
        }
        FileLog.e("emm", "userid=========" + UserConfig.clientUserId);
        mProximitySensorManager = new ProximitySensorManager(
                this.getActivity(), mProximitySensorListener, false);
        SharedPreferences preferences = ApplicationLoader.applicationContext
                .getSharedPreferences("mainconfig_"
                                + UserConfig.clientUserId,
                        Activity.MODE_PRIVATE);
        MessagesController.getInstance().earphone = preferences.getBoolean("earphone", false);
        mToast = Toast.makeText(this.parentActivity, "", Toast.LENGTH_SHORT);
    }

    /**
     * 初始化监听器。
     */
    private InitListener mInitListener = new InitListener() {

        @Override
        public void onInit(int code) {
            Log.d(TAG, "SpeechRecognizer init() code = " + code);
            if (code == ErrorCode.SUCCESS) {
                //findViewById(R.id.iat_recognize).setEnabled(true);
            }
            //showTip("SpeechRecognizer init() code = " + code);
        }
    };

    public static String parseIatResult(String json) {
        StringBuffer ret = new StringBuffer();
        try {
            JSONTokener tokener = new JSONTokener(json);
            JSONObject joResult = new JSONObject(tokener);

            JSONArray words = joResult.getJSONArray("ws");
            for (int i = 0; i < words.length(); i++) {
                // 转写锟斤拷锟斤拷剩锟侥拷锟绞癸拷玫锟揭伙拷锟斤拷锟斤拷
                JSONArray items = words.getJSONObject(i).getJSONArray("cw");
                JSONObject obj = items.getJSONObject(0);
                ret.append(obj.getString("w"));
                //				锟斤拷锟斤拷锟揭拷锟斤拷选锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷侄锟�
                //				for(int j = 0; j < items.length(); j++)
                //				{
                //					JSONObject obj = items.getJSONObject(j);
                //					ret.append(obj.getString("w"));
                //				}
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret.toString();
    }

    /**
     * 参数设置
     *
     * @param param
     * @return
     */
    @SuppressLint("SdCardPath")
    public void setParam() {
        String lan = LocaleController.getCurrentLanguageName();
        if (lan.contains("简体中文")) {
            mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
            mIat.setParameter(SpeechConstant.ACCENT, "mandarin");
        } else
            mIat.setParameter(SpeechConstant.LANGUAGE, "en_us");
        // 设置语音前端点
        mIat.setParameter(SpeechConstant.VAD_BOS, "4000");
        // 设置语音后端点
        mIat.setParameter(SpeechConstant.VAD_EOS, "2000");
        // 设置标点符号
        mIat.setParameter(SpeechConstant.ASR_PTT, "1");
        // 设置音频保存路径
        //mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH, "/sdcard/iflytek/wavaudio.pcm");
    }

    /**
     * 听写UI监听器
     */
    private RecognizerDialogListener recognizerDialogListener = new RecognizerDialogListener() {
        public void onResult(RecognizerResult results, boolean isLast) {
            String text = parseIatResult(results.getResultString());
            mResultText += text;
            if (isLast) {
                int index = messsageEditText.getSelectionStart();
                messsageEditText.getText().insert(index, mResultText);
                mResultText = "";
            }
        }

        /**
         * 识别回调错误
         */
        public void onError(SpeechError error) {
            showTip(error.getPlainDescription(true));
        }

    };


    /**
     * 听写监听器。
     */
    private RecognizerListener recognizerListener = new RecognizerListener() {

        @Override
        public void onBeginOfSpeech() {
            //showTip("锟斤拷始说锟斤拷");
        }


        @Override
        public void onError(SpeechError error) {
            showTip(error.getPlainDescription(true));
        }

        @Override
        public void onEndOfSpeech() {
            //showTip("锟斤拷锟斤拷说锟斤拷");
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, String msg) {

        }

        @Override
        public void onResult(RecognizerResult results, boolean isLast) {
            String text = parseIatResult(results.getResultString());
            mResultText += text;
            if (isLast) {
                messsageEditText.setText(mResultText);
            }
        }

        @Override
        public void onVolumeChanged(int volume) {
            //showTip("锟斤拷前锟斤拷锟斤拷说锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷小锟斤拷" + volume);
        }

    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        FileLog.e("emm", "chat onCreateView");
        if (fragmentView == null) {
            fragmentView = inflater.inflate(R.layout.chat_layout, container,
                    false);

            sizeNotifierRelativeLayout = (SizeNotifierRelativeLayout) fragmentView
                    .findViewById(R.id.chat_layout);
            sizeNotifierRelativeLayout.delegate = this;
            contentView = sizeNotifierRelativeLayout;

            emptyView = (TextView) fragmentView
                    .findViewById(R.id.searchEmptyView);
            emptyView.setText(LocaleController.getString("NoMessages",
                    R.string.NoMessages));//没锟斤拷锟斤拷息
            chatListView = (LayoutListView) fragmentView
                    .findViewById(R.id.chat_list_view);
            chatListView.setAdapter(chatAdapter = new ChatAdapter(
                    parentActivity));
            topPanel = fragmentView.findViewById(R.id.top_panel);
            topPlaneClose = (ImageView) fragmentView
                    .findViewById(R.id.top_plane_close);
            topPanelText = (TextView) fragmentView
                    .findViewById(R.id.top_panel_text);
            bottomOverlay = fragmentView.findViewById(R.id.bottom_overlay);
            bottomOverlayText = (TextView) fragmentView
                    .findViewById(R.id.bottom_overlay_text);
            bottomOverlayChat = fragmentView
                    .findViewById(R.id.bottom_overlay_chat);
            progressView = fragmentView.findViewById(R.id.progressLayout);
            pagedownButton = fragmentView.findViewById(R.id.pagedown_button);
            audioSendButton = (ImageButton) fragmentView
                    .findViewById(R.id.chat_audio_send_button);//锟斤拷锟斤拷锟斤拷锟斤拷
            recordPanel = fragmentView.findViewById(R.id.record_panel);
            recordTimeText = (TextView) fragmentView
                    .findViewById(R.id.recording_time_text);//锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷时锟斤拷
            View progressViewInner = progressView
                    .findViewById(R.id.progressLayoutInner);

            updateContactStatus();

            ImageView backgroundImage = (ImageView) fragmentView
                    .findViewById(R.id.background_image);

            SharedPreferences preferences = ApplicationLoader.applicationContext
                    .getSharedPreferences("mainconfig_"
                            + UserConfig.clientUserId, Activity.MODE_PRIVATE);
            int selectedBackground = preferences.getInt("selectedBackground",
                    1000001);
            int selectedColor = preferences.getInt("selectedColor", 0);
            if (selectedColor != 0) {//锟斤拷锟斤拷锟斤拷色
                backgroundImage.setBackgroundColor(selectedColor);
                chatListView.setCacheColorHint(selectedColor);
            } else {
                chatListView.setCacheColorHint(0);
                if (selectedBackground == 1000001) {
                    backgroundImage.setImageResource(R.drawable.background_hd);
                } else {
                    File toFile = new File(
                            ApplicationLoader.applicationContext.getFilesDir(),
                            "wallpaper.jpg");
                    if (toFile.exists()) {
                        if (ApplicationLoader.cachedWallpaper != null) {
                            backgroundImage
                                    .setImageBitmap(ApplicationLoader.cachedWallpaper);
                        } else {
                            backgroundImage.setImageURI(Uri.fromFile(toFile));
                            if (backgroundImage.getDrawable() instanceof BitmapDrawable) {
                                ApplicationLoader.cachedWallpaper = ((BitmapDrawable) backgroundImage
                                        .getDrawable()).getBitmap();
                            }
                        }
                        isCustomTheme = true;
                    } else {
                        backgroundImage
                                .setImageResource(R.drawable.background_hd);
                    }
                }
            }

            if (currentEncryptedChat != null) {
                secretChatPlaceholder = contentView
                        .findViewById(R.id.secret_placeholder);  //时锟斤拷
                if (isCustomTheme) {
                    secretChatPlaceholder
                            .setBackgroundResource(R.drawable.system_black);//时锟斤拷谋锟斤拷锟斤拷锟缴�
                } else {
                    secretChatPlaceholder
                            .setBackgroundResource(R.drawable.system_blue);
                }
                secretViewStatusTextView = (TextView) contentView
                        .findViewById(R.id.invite_text);//时锟斤拷锟斤拷锟斤拷锟�
                secretChatPlaceholder.setPadding(Utilities.dp(16),
                        Utilities.dp(12), Utilities.dp(16), Utilities.dp(12));//锟斤拷锟斤拷锟斤拷锟斤拷

                View v = contentView.findViewById(R.id.secret_placeholder);
                v.setVisibility(View.VISIBLE);

                if (currentEncryptedChat.admin_id == UserConfig.clientUserId) {
                    if (currentUser.first_name.length() > 0) {
                        secretViewStatusTextView
                                .setText(LocaleController
                                        .formatString(
                                                "EncryptedPlaceholderTitleOutgoing",
                                                R.string.EncryptedPlaceholderTitleOutgoing,
                                                currentUser.first_name));
                    } else {
                        secretViewStatusTextView
                                .setText(LocaleController
                                        .formatString(
                                                "EncryptedPlaceholderTitleOutgoing",
                                                R.string.EncryptedPlaceholderTitleOutgoing,
                                                currentUser.last_name));
                    }
                } else {
                    if (currentUser.first_name.length() > 0) {
                        secretViewStatusTextView
                                .setText(LocaleController
                                        .formatString(
                                                "EncryptedPlaceholderTitleIncoming",
                                                R.string.EncryptedPlaceholderTitleIncoming,
                                                currentUser.first_name));
                    } else {
                        secretViewStatusTextView
                                .setText(LocaleController
                                        .formatString(
                                                "EncryptedPlaceholderTitleIncoming",
                                                R.string.EncryptedPlaceholderTitleIncoming,
                                                currentUser.last_name));
                    }
                }

                updateSecretStatus();
            }

            if (isCustomTheme) {
                progressViewInner
                        .setBackgroundResource(R.drawable.system_loader2);
                emptyView.setBackgroundResource(R.drawable.system_black);
            } else {
                progressViewInner
                        .setBackgroundResource(R.drawable.system_loader1);
                emptyView.setBackgroundResource(R.drawable.system_blue);
            }
            emptyView.setPadding(Utilities.dp(7), Utilities.dp(1),
                    Utilities.dp(7), Utilities.dp(1));

            if (currentUser != null && currentUser.id / 1000 == 333) {
                emptyView.setText(LocaleController.getString("GotAQuestion",
                        R.string.GotAQuestion));
            }

            chatListView
                    .setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                        @Override
                        public boolean onItemLongClick(AdapterView<?> adapter,
                                                       View view, int position, long id) {
                            if (mActionMode == null) {
                                createMenu(view, false);
                            }
                            return true;
                        }
                    });

            final Rect scrollRect = new Rect();

            chatListView
                    .setOnScrollListener(new AbsListView.OnScrollListener() {
                        @Override
                        public void onScrollStateChanged(
                                AbsListView absListView, int i) {

                        }

                        @Override
                        public void onScroll(AbsListView absListView,
                                             int firstVisibleItem, int visibleItemCount,
                                             int totalItemCount) {
                            if (visibleItemCount > 0) {
                                if (firstVisibleItem <= 4) {
                                    if (!endReached && !loading) {
                                        if (messagesByDays.size() != 0) {
                                            MessagesController.getInstance()
                                                    .loadMessages(dialog_id, 0,
                                                            20, maxMessageId,
                                                            !cacheEndReaced,
                                                            minDate, classGuid,
                                                            false, false);
                                        } else {
                                            MessagesController.getInstance()
                                                    .loadMessages(dialog_id, 0,
                                                            20, 0,
                                                            !cacheEndReaced,
                                                            minDate, classGuid,
                                                            false, false);
                                        }
                                        loading = true;
                                    }
                                }
                                if (firstVisibleItem + visibleItemCount >= totalItemCount - 6) {
                                    if (!unread_end_reached && !loadingForward) {
                                        MessagesController.getInstance()
                                                .loadMessages(dialog_id, 0, 20,
                                                        minMessageId, true,
                                                        maxDate, classGuid,
                                                        false, true);
                                        loadingForward = true;
                                    }
                                }
                                if (firstVisibleItem + visibleItemCount == totalItemCount
                                        && unread_end_reached) {
                                    showPagedownButton(false, true);
                                }
                            } else {
                                showPagedownButton(false, false);
                            }
                            for (int a = 0; a < visibleItemCount; a++) {
                                View view = absListView.getChildAt(a);
                                if (view instanceof ChatMessageCell) {
                                    ChatMessageCell messageCell = (ChatMessageCell) view;
                                    messageCell.getLocalVisibleRect(scrollRect);
                                    messageCell.setVisiblePart(scrollRect.top,
                                            scrollRect.bottom - scrollRect.top);
                                } else {

                                }
                            }
                        }
                    });

            messsageEditText = (EditText) fragmentView
                    .findViewById(R.id.chat_text_edit);
            messsageEditText.setHint(LocaleController.getString("TypeMessage",
                    R.string.TypeMessage));
            slideText = fragmentView.findViewById(R.id.slideText);
            TextView textView = (TextView) fragmentView
                    .findViewById(R.id.slideToCancelTextView);
            textView.setText(LocaleController.getString("SlideToCancel",
                    R.string.SlideToCancel));
            textView = (TextView) fragmentView
                    .findViewById(R.id.bottom_overlay_chat_text);
            textView.setText(LocaleController.getString("DeleteThisGroup",
                    R.string.DeleteThisGroup));
            textView = (TextView) fragmentView.findViewById(R.id.secret_title);
            textView.setText(LocaleController.getString(
                    "EncryptedDescriptionTitle",
                    R.string.EncryptedDescriptionTitle));
            textView = (TextView) fragmentView
                    .findViewById(R.id.secret_description1);
            textView.setText(LocaleController.getString(
                    "EncryptedDescription1", R.string.EncryptedDescription1));
            textView = (TextView) fragmentView
                    .findViewById(R.id.secret_description2);
            textView.setText(LocaleController.getString(
                    "EncryptedDescription2", R.string.EncryptedDescription2));
            textView = (TextView) fragmentView
                    .findViewById(R.id.secret_description3);
            textView.setText(LocaleController.getString(
                    "EncryptedDescription3", R.string.EncryptedDescription3));
            textView = (TextView) fragmentView
                    .findViewById(R.id.secret_description4);
            textView.setText(LocaleController.getString(
                    "EncryptedDescription4", R.string.EncryptedDescription4));

            sendButton = (ImageButton) fragmentView
                    .findViewById(R.id.chat_send_button);
            sendButton.setEnabled(false);
            sendButton.setVisibility(View.INVISIBLE);
            emojiButton = (ImageView) fragmentView
                    .findViewById(R.id.chat_smile_button);

            if (loading && messages.isEmpty()) {
                progressView.setVisibility(View.VISIBLE);
                chatListView.setEmptyView(null);
            } else {
                progressView.setVisibility(View.GONE);
                if (currentEncryptedChat == null) {
                    chatListView.setEmptyView(emptyView);
                } else {
                    chatListView.setEmptyView(secretChatPlaceholder);
                }
            }

            emojiButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (emojiPopup == null) {
                        showEmojiPopup(true);
                    } else {
                        showEmojiPopup(!emojiPopup.isShowing());
                    }
                }
            });

            messsageEditText.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View view, int i, KeyEvent keyEvent) {
                    if (i == 4 && !keyboardVisible && emojiPopup != null
                            && emojiPopup.isShowing()) {//锟斤拷锟斤拷锟斤拷
                        if (keyEvent.getAction() == 1) {
                            showEmojiPopup(false);
                        }
                        return true;
                    } else if (i == KeyEvent.KEYCODE_ENTER && sendByEnter
                            && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {//锟斤拷锟斤拷锟斤拷
                        sendMessage();
                        return true;
                    }
                    return false;
                }
            });

            messsageEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView textView, int i,
                                              KeyEvent keyEvent) {
                    if (i == EditorInfo.IME_ACTION_UNSPECIFIED) {
                        String message = messsageEditText.getText()
                                .toString().trim();
                        if (message.equals("")) {
                            return true;
                        }
                    }
                    if (i == EditorInfo.IME_ACTION_SEND) {
                        sendMessage();
                        return true;
                    } else if (sendByEnter) {
                        if (keyEvent != null
                                && i == EditorInfo.IME_NULL
                                && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                            sendMessage();
                            return true;
                        }
                    }
                    return false;
                }
            });

            sendButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    sendMessage();
                }
            });

            audioSendButton.setOnTouchListener(new View.OnTouchListener() {
                @SuppressLint("NewApi")
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    if (WeiyiMeeting.isInMeeting()) {
                        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                            UiUtil.showToastForChat(parentActivity, R.string.toast_msg_have_a_call);
                        }
                        return true;
                    }
                    if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                        startedDraggingX = -1;
                        if (android.os.Build.VERSION.SDK_INT >= 11) {
                            UEngine.getInstance().getSoundService().playMidSound(1);
                        }
                        MediaController.getInstance().startRecording(dialog_id);
                        updateAudioRecordIntefrace();
                    } else if (motionEvent.getAction() == MotionEvent.ACTION_UP
                            || motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {
                        showConnectStatus();// xueqiang add for tip
                        startedDraggingX = -1;
                        MediaController.getInstance().stopRecording(true);
                        recordingAudio = false;
                        updateAudioRecordIntefrace();
                    } else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE
                            && recordingAudio) {
                        float x = motionEvent.getX();
                        if (x < -distCanMove) {
                            MediaController.getInstance().stopRecording(false);
                            recordingAudio = false;
                            updateAudioRecordIntefrace();
                        }
                        if (android.os.Build.VERSION.SDK_INT > 13) {
                            x = x + audioSendButton.getX();
                            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) slideText
                                    .getLayoutParams();
                            if (startedDraggingX != -1) {
                                float dist = (x - startedDraggingX);
                                params.leftMargin = Utilities.dp(30)
                                        + (int) dist;
                                slideText.setLayoutParams(params);
                                float alpha = 1.0f + dist / distCanMove;
                                if (alpha > 1) {
                                    alpha = 1;
                                } else if (alpha < 0) {
                                    alpha = 0;
                                }
                                slideText.setAlpha(alpha);
                            }
                            if (x <= slideText.getX() + slideText.getWidth()
                                    + Utilities.dp(30)) {
                                if (startedDraggingX == -1) {
                                    startedDraggingX = x;
                                    distCanMove = (recordPanel
                                            .getMeasuredWidth()
                                            - slideText.getMeasuredWidth() - Utilities
                                            .dp(48)) / 2.0f;
                                    if (distCanMove <= 0) {
                                        distCanMove = Utilities.dp(80);
                                    } else if (distCanMove > Utilities.dp(80)) {
                                        distCanMove = Utilities.dp(80);
                                    }
                                }
                            }
                            if (params.leftMargin > Utilities.dp(30)) {
                                params.leftMargin = Utilities.dp(30);
                                slideText.setLayoutParams(params);
                                slideText.setAlpha(1);
                                startedDraggingX = -1;
                            }
                        }
                    }
                    view.onTouchEvent(motionEvent);
                    return true;
                }
            });

            pagedownButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    scrollToLastMessage();
                }
            });

            checkSendButton();

            messsageEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i,
                                              int i2, int i3) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i,
                                          int i2, int i3) {
                    String message = charSequence.toString().trim();
                    message = message.replaceAll("\n\n+", "\n\n");
                    message = message.replaceAll(" +", " ");
                    sendButton.setEnabled(message.length() != 0);
                    checkSendButton();

                    if (message.length() != 0
                            && lastTypingTimeSend < System.currentTimeMillis() - 5000
                            && !ignoreTextChange) {
                        int currentTime = ConnectionsManager.getInstance()
                                .getCurrentTime();
                        if (currentUser != null && currentUser.status != null
                                && currentUser.status.expires < currentTime) {
                            return;
                        }
                        lastTypingTimeSend = System.currentTimeMillis();
                        MessagesController.getInstance().sendTyping(dialog_id,
                                classGuid);
                    }
                }

                @Override
                public void afterTextChanged(Editable editable) {
                    if (sendByEnter && editable.length() > 0
                            && editable.charAt(editable.length() - 1) == '\n') {
                        sendMessage();
                    }
                    int i = 0;
                    ImageSpan[] arrayOfImageSpan = editable.getSpans(0,
                            editable.length(), ImageSpan.class);
                    int j = arrayOfImageSpan.length;
                    while (true) {
                        if (i >= j) {
                            Emoji.replaceEmoji(editable);
                            return;
                        }
                        editable.removeSpan(arrayOfImageSpan[i]);
                        i++;
                    }
                }
            });

            bottomOverlayChat.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (currentChat != null) {
                        MessagesController.getInstance().deleteDialog(
                                -currentChat.id, 0, false);
                        finishFragment();
                    }
                }
            });

            chatListView
                    .setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> adapterView,
                                                View view, int i, long l) {
                            if (view instanceof ChatReminderCell && ((ChatReminderCell) view).isInTouch) {
                                ((ChatReminderCell) view).mEvent.listenerTouch();
                                ((ChatReminderCell) view).isInTouch = false;
                                return;
                            }
                            if (mActionMode != null) {
                                processRowSelect(view);
                                return;
                            }
                            createMenu(view, true);
                        }
                    });

            chatListView.setOnTouchListener(new OnSwipeTouchListener() {
                public void onSwipeRight() {
                    try {
                        if (visibleDialog != null) {
                            visibleDialog.dismiss();
                            visibleDialog = null;
                        }
                    } catch (Exception e) {
                        FileLog.e("emm", e);
                    }
                    finishFragment(true);
                }

                public void onSwipeLeft() {
                    if (swipeOpening) {
                        return;
                    }
                    try {
                        if (visibleDialog != null) {
                            visibleDialog.dismiss();
                            visibleDialog = null;
                        }
                    } catch (Exception e) {
                        FileLog.e("emm", e);
                    }
                    if (avatarImageView != null) {
                        swipeOpening = true;
                        avatarImageView.performClick();
                    }
                }
            });

            emptyView.setOnTouchListener(new OnSwipeTouchListener() {
                public void onSwipeRight() {
                    finishFragment(true);
                }

                public void onSwipeLeft() {
                    if (swipeOpening) {
                        return;
                    }
                    if (avatarImageView != null) {
                        swipeOpening = true;
                        avatarImageView.performClick();
                    }
                }
            });
            if (currentChat != null
                    && (currentChat instanceof TLRPC.TL_chatForbidden || currentChat.left)) {
                bottomOverlayChat.setVisibility(View.VISIBLE);
            } else {
                bottomOverlayChat.setVisibility(View.GONE);
            }
        } else {
            ViewGroup parent = (ViewGroup) fragmentView.getParent();
            if (parent != null) {
                parent.removeView(fragmentView);
            }

            // jenf
            if (null != currentChat) {
                currentChat = MessagesController.getInstance().chats
                        .get(currentChat.id);
            }
        }
        // xueqiang add for network tip
        showConnectStatus();
        callViewSet();
        return fragmentView;
    }

    private void startMeeting(String mid) {
        //caller锟斤拷要锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟叫讹拷锟角凤拷锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷OK锟剿ｏ拷锟斤拷锟斤拷羌锟绞憋拷锟斤拷锟斤拷锟矫伙拷锟接︼拷梅锟斤拷锟斤拷锟叫ｏ拷同时锟斤拷锟斤拷锟斤拷锟斤拷锟�
        //锟斤拷1锟斤拷1锟竭硷拷锟斤拷同,锟斤拷锟矫达拷锟斤拷iscaller锟斤拷锟斤拷锟斤拷锟斤拷锟�
        if (getActivity() != null) {
            Utitlties.ShowProgressDialog(getActivity(), getResources().getString(R.string.Loading));
            ApplicationLoader.getInstance().joinInstMeeting(this.getActivity(), mid, (int) dialog_id);
        }
    }

    /**
     * 锟剿会话锟斤拷锟斤拷 同锟斤拷锟节讹拷锟斤拷锟斤拷频通锟斤拷 锟斤拷示锟斤拷锟斤拷锟斤拷锟斤拷
     */
    private void callViewSet() {
        if (callLayout == null) {
            if (fragmentView != null) {
                callLayout = (LinearLayout) fragmentView
                        .findViewById(R.id.linlay_call);
                callInfo = (TextView) fragmentView
                        .findViewById(R.id.tv_callinfo);
                callLayout.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //checkmeeting应锟矫凤拷锟截革拷锟斤拷一锟斤拷meetingid,xueqiang tod...
                        String mid = IMRtmpClientMgr.getInstance().getMid(currentChat.id);
                        startMeeting(mid);
                    }
                });
            }
        }

		/*if(currentUser!=null && meeting.getUserId()==currentUser.id)
        {
			if(meeting.inMeeting)
			{
				callLayout.setVisibility(View.VISIBLE);
				String temp = LocaleController.getString("voicechat", R.string.voicechat);
				setCallText(temp);
				return;
			}
		}
		if(currentChat!=null && meeting.getChatId()==currentChat.id)
		{
			if(meeting.inMeeting)
			{
				//锟揭伙拷锟节伙拷锟斤拷锟斤拷只锟角斤拷锟斤拷没锟斤拷锟斤拷
				callLayout.setVisibility(View.VISIBLE);
				String meetingStatus = meeting.getStatus(currentChat.id);
				setCallText(meetingStatus);
				return;
			}
		}*/
        if (currentChat != null) {
            String meetingStatus = IMRtmpClientMgr.getInstance().getStatus(currentChat.id);
            if (!meetingStatus.isEmpty()) {
                callLayout.setVisibility(View.VISIBLE);
                setCallText(meetingStatus);
                return;
            }
        }
        if (callLayout != null)
            callLayout.setVisibility(View.GONE);
    }

    private void setCallText(String msg) {
        TextView callView = callInfo;
        if (callView != null && !StringUtil.isEmpty(msg)) {
            callView.setText(msg);
        }
    }

    /**
     * 选锟斤拷锟斤拷锟斤拷息锟侥帮拷钮
     */
    private void checkSendButton() {
        String message = messsageEditText.getText().toString().trim();
        message = message.replaceAll("\n\n+", "\n\n");
        message = message.replaceAll(" +", " ");
        if (message.length() > 0) {
            sendButton.setVisibility(View.VISIBLE);
            audioSendButton.setVisibility(View.INVISIBLE);
        } else {
            sendButton.setVisibility(View.INVISIBLE);
            audioSendButton.setVisibility(View.VISIBLE);
        }
    }

    private void updateAudioRecordIntefrace() {
        if (parentActivity == null) {
            return;
        }
        if (recordingAudio) {
            try {
                if (mWakeLock == null) {
                    PowerManager pm = (PowerManager) parentActivity
                            .getSystemService(Context.POWER_SERVICE);
                    mWakeLock = pm.newWakeLock(
                            PowerManager.SCREEN_DIM_WAKE_LOCK
                                    | PowerManager.ON_AFTER_RELEASE,
                            "audio record lock");
                    mWakeLock.acquire();
                }
            } catch (Exception e) {
                FileLog.e("emm", e);
            }
            try {
                prevOrientation = parentActivity.getRequestedOrientation();
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    parentActivity
                            .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    parentActivity
                            .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                } else {
                    parentActivity
                            .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
                }
            } catch (Exception e) {
                FileLog.e("emm", e);
            }

            recordPanel.setVisibility(View.VISIBLE);
            recordTimeText.setText("00:00");
            lastTimeString = null;
            if (android.os.Build.VERSION.SDK_INT > 13) {
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) slideText
                        .getLayoutParams();
                params.leftMargin = Utilities.dp(30);
                slideText.setLayoutParams(params);
                slideText.setAlpha(1);
                recordPanel.setX(Utilities.displaySize.x);
                recordPanel
                        .animate()
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .setListener(new Animator.AnimatorListener() {
                            @SuppressLint("NewApi")
                            @Override
                            public void onAnimationStart(Animator animator) {
                            }

                            @SuppressLint("NewApi")
                            @Override
                            public void onAnimationEnd(Animator animator) {
                                recordPanel.setX(0);
                            }

                            @Override
                            public void onAnimationCancel(Animator animator) {
                            }

                            @Override
                            public void onAnimationRepeat(Animator animator) {
                            }
                        }).setDuration(300).translationX(0).start();
            }
        } else {
            if (mWakeLock != null) {
                try {
                    mWakeLock.release();
                    mWakeLock = null;
                } catch (Exception e) {
                    FileLog.e("emm", e);
                }
            }
            try {
                if (prevOrientation != -10) {
                    parentActivity.setRequestedOrientation(prevOrientation);
                    prevOrientation = -10;
                }
            } catch (Exception e) {
                FileLog.e("emm", e);
            }
            if (android.os.Build.VERSION.SDK_INT > 13) {
                recordPanel
                        .animate()
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .setListener(new Animator.AnimatorListener() {
                            @Override
                            public void onAnimationStart(Animator animator) {

                            }

                            @Override
                            public void onAnimationEnd(Animator animator) {
                                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) slideText
                                        .getLayoutParams();
                                params.leftMargin = Utilities.dp(30);
                                slideText.setLayoutParams(params);
                                slideText.setAlpha(1);
                                recordPanel.setVisibility(View.GONE);
                            }

                            @Override
                            public void onAnimationCancel(Animator animator) {
                            }

                            @Override
                            public void onAnimationRepeat(Animator animator) {
                            }
                        }).setDuration(300)
                        .translationX(Utilities.displaySize.x).start();
            } else {
                recordPanel.setVisibility(View.GONE);
            }
        }
    }

    /**
     * 锟斤拷锟斤拷息
     */
    private void sendMessage() {
        // xueqiang add
        showConnectStatus();

        String message = messsageEditText.getText().toString().trim();
        if (processSendingText(message)) {
            mResultText = "";
            messsageEditText.setText("");
            lastTypingTimeSend = 0;
            chatListView.post(new Runnable() {
                @Override
                public void run() {
                    chatListView.setSelectionFromTop(messages.size() - 1,
                            -100000 - chatListView.getPaddingTop());
                }
            });
        }
    }

    private void scrollToLastMessage() {
        if (unread_end_reached || first_unread_id == 0) {
            chatListView.setSelectionFromTop(messages.size() - 1, -100000
                    - chatListView.getPaddingTop());
        } else {
            messages.clear();
            messagesByDays.clear();
            messagesDict.clear();
            progressView.setVisibility(View.VISIBLE);
            chatListView.setEmptyView(null);
            if (currentEncryptedChat == null) {
                maxMessageId = Integer.MAX_VALUE;
                minMessageId = Integer.MIN_VALUE;
            } else {
                maxMessageId = Integer.MIN_VALUE;
                minMessageId = Integer.MAX_VALUE;
            }
            maxDate = Integer.MIN_VALUE;
            minDate = 0;
            unread_end_reached = true;
            MessagesController.getInstance().loadMessages(dialog_id, 0, 30, 0,
                    true, 0, classGuid, true, false);
            loading = true;
            chatAdapter.notifyDataSetChanged();
        }
    }

    private void showPagedownButton(boolean show, boolean animated) {
        if (pagedownButton == null) {
            return;
        }
        if (show) {
            if (pagedownButton.getVisibility() == View.GONE) {
                if (android.os.Build.VERSION.SDK_INT >= 16 && animated) {
                    pagedownButton.setVisibility(View.VISIBLE);
                    pagedownButton.setAlpha(0);
                    pagedownButton.animate().alpha(1).setDuration(200).start();
                } else {
                    pagedownButton.setVisibility(View.VISIBLE);
                }
            }
        } else {
            if (pagedownButton.getVisibility() == View.VISIBLE) {
                if (android.os.Build.VERSION.SDK_INT >= 16 && animated) {
                    pagedownButton.animate().alpha(0)
                            .withEndAction(new Runnable() {
                                @Override
                                public void run() {
                                    pagedownButton.setVisibility(View.GONE);
                                }
                            }).setDuration(200).start();
                } else {
                    pagedownButton.setVisibility(View.GONE);
                }
            }
        }
    }

    @Override
    public void onAnimationEnd() {
        super.onAnimationEnd();
        if (invalidateAfterAnimation) {
            if (chatListView != null) {
                updateVisibleRows();
            }
        }
    }

    @Override
    public void willBeHidden() {
        super.willBeHidden();
        paused = true;
    }

    private void updateSecretStatus() {
        if (bottomOverlay == null) {
            return;
        }
        if (currentEncryptedChat == null || secretViewStatusTextView == null) {
            bottomOverlay.setVisibility(View.GONE);
            return;
        }
        boolean hideKeyboard = false;
        if (currentEncryptedChat instanceof TLRPC.TL_encryptedChatRequested) {
            bottomOverlayText.setText(LocaleController.getString(
                    "EncryptionProcessing", R.string.EncryptionProcessing));
            bottomOverlay.setVisibility(View.VISIBLE);
            hideKeyboard = true;
        } else if (currentEncryptedChat instanceof TLRPC.TL_encryptedChatWaiting) {
            bottomOverlayText.setText(Html.fromHtml(LocaleController
                    .formatString("AwaitingEncryption",
                            R.string.AwaitingEncryption, "<b>"
                                    + currentUser.first_name + "</b>")));
            bottomOverlay.setVisibility(View.VISIBLE);
            hideKeyboard = true;
        } else if (currentEncryptedChat instanceof TLRPC.TL_encryptedChatDiscarded) {
            bottomOverlayText.setText(LocaleController.getString(
                    "EncryptionRejected", R.string.EncryptionRejected));
            bottomOverlay.setVisibility(View.VISIBLE);
            hideKeyboard = true;
        } else if (currentEncryptedChat instanceof TLRPC.TL_encryptedChat) {
            bottomOverlay.setVisibility(View.GONE);
        }
        if (hideKeyboard) {
            hideEmojiPopup();
            if (parentActivity != null) {
                Utilities.hideKeyboard(parentActivity.getCurrentFocus());
            }
        }
        if (parentActivity != null) {
            parentActivity.supportInvalidateOptionsMenu();
        }
    }

    private void addToLoadingFile(String path, ProgressBar bar) {
        ArrayList<ProgressBar> arr = loadingFile.get(path);
        if (arr == null) {
            arr = new ArrayList<ProgressBar>();
            loadingFile.put(path, arr);
        }
        arr.add(bar);
    }

    private void removeFromloadingFile(String path, ProgressBar bar) {
        ArrayList<ProgressBar> arr = loadingFile.get(path);
        if (arr != null) {
            arr.remove(bar);
        }
    }

    private void updateOnlineCount() {
        if (chatinfo == null) {
            return;
        }
        onlineCount = 0;
        int currentTime = ConnectionsManager.getInstance().getCurrentTime();
        for (TLRPC.TL_chatParticipant participant : chatinfo.participants) {
            TLRPC.User user = MessagesController.getInstance().users
                    .get(participant.user_id);
            if (user != null
                    && user.status != null
                    && (user.status.expires > currentTime || user.id == UserConfig.clientUserId)
                    && user.status.expires > 10000) {
                onlineCount++;
            }
        }

        updateSubtitle();
    }

    private int getMessageType(MessageObject messageObject) {
        if (currentEncryptedChat == null) {
            if (messageObject.messageOwner.id <= 0
                    && messageObject.messageOwner.out) {
                if (messageObject.messageOwner.send_state == MessagesController.MESSAGE_SEND_STATE_SEND_ERROR) {
                    return 0;
                } else {
                    return -1;
                }
            } else {
                if (messageObject.type == 15) {
                    return -1;
                } else if (messageObject.type == 10 || messageObject.type == 11) {
                    if (messageObject.messageOwner.id == 0) {
                        return -1;
                    }
                    return 1;
                } else if (messageObject.type == 20 || messageObject.type == 21) {
                    return 5;
                } else {
                    if (!(messageObject.messageOwner.media instanceof TLRPC.TL_messageMediaEmpty)) {
                        if (messageObject.messageOwner.media instanceof TLRPC.TL_messageMediaVideo
                                || messageObject.messageOwner.media instanceof TLRPC.TL_messageMediaPhoto
                                || messageObject.messageOwner.media instanceof TLRPC.TL_messageMediaDocument) {
                            File f = new File(Utilities.getCacheDir(),
                                    messageObject.getFileName());
                            if (f.exists()) {
                                return 4;
                            }
                        }
                        return 2;
                    } else {
                        return 3;
                    }
                }
            }
        } else {
            if (messageObject.type == 15) {
                return -1;
            } else if (messageObject.messageOwner.send_state == MessagesController.MESSAGE_SEND_STATE_SEND_ERROR) {
                return 0;
            } else if (messageObject.type == 10
                    || messageObject.type == 11
                    || messageObject.messageOwner.send_state == MessagesController.MESSAGE_SEND_STATE_SENDING) {
                if (messageObject.messageOwner.id == 0) {
                    return -1;
                }
                return 1;
            } else {
                if (!(messageObject.messageOwner.media instanceof TLRPC.TL_messageMediaEmpty)) {
                    /*
                     * if (messageObject.messageOwner.media instanceof
					 * TLRPC.TL_messageMediaVideo ||
					 * messageObject.messageOwner.media instanceof
					 * TLRPC.TL_messageMediaPhoto) { File f = new
					 * File(Utilities.getCacheDir(),
					 * messageObject.getFileName()); if (f.exists()) { return 4;
					 * } }
					 */
                    return 2;
                } else {
                    return 3;
                }
            }
        }
    }

    private void addToSelectedMessages(MessageObject messageObject) {
        if (selectedMessagesIds.containsKey(messageObject.messageOwner.id)) {
            selectedMessagesIds.remove(messageObject.messageOwner.id);
            if (messageObject.type == 0 || messageObject.type == 1
                    || messageObject.type == 8 || messageObject.type == 9) {
                selectedMessagesCanCopyIds
                        .remove(messageObject.messageOwner.id);
            }
            if (messageObject.type == 20 || messageObject.type == 21) {
                selectedMessagesCanNotForwardIds
                        .remove(messageObject.messageOwner.id);
            }
        } else {
            selectedMessagesIds.put(messageObject.messageOwner.id,
                    messageObject);
            if (messageObject.type == 0 || messageObject.type == 1
                    || messageObject.type == 8 || messageObject.type == 9) {
                selectedMessagesCanCopyIds.put(messageObject.messageOwner.id,
                        messageObject);
            }
            if (messageObject.type == 20 || messageObject.type == 21) {
                selectedMessagesCanNotForwardIds.put(messageObject.messageOwner.id,
                        messageObject);
            }
        }
        if (mActionMode != null && mActionMode.getMenu() != null) {
            mActionMode.getMenu().findItem(R.id.copy)
                    .setVisible(selectedMessagesCanCopyIds.size() != 0);
            if (mActionMode.getMenu().findItem(R.id.forward) != null) {
                if (selectedMessagesCanNotForwardIds.size() > 0) {
                    mActionMode.getMenu().findItem(R.id.forward).setVisible(false);
                    mActionMode.getMenu().findItem(R.id.copy).setVisible(false);
                }

            }
        }
    }

    private void processRowSelect(View view) {
        View parentView = getRowParentView(view);
        if (parentView == null) {
            return;
        }
        MessageObject message = null;
        if (view instanceof ChatBaseCell) {
            message = ((ChatBaseCell) view).getMessageObject();
        } else {
            ChatListRowHolderEx holder = (ChatListRowHolderEx) parentView
                    .getTag();
            message = holder.message;
        }

        if (getMessageType(message) < 2) {
            return;
        }
        addToSelectedMessages(message);
        updateActionModeTitle();
        updateVisibleRows();
    }

    private void updateActionModeTitle() {
        if (mActionMode == null) {
            return;
        }
        if (selectedMessagesIds.isEmpty()) {
            mActionMode.finish();
        } else {
            mActionMode.setTitle(String.format("%s %d",
                    LocaleController.getString("Selected", R.string.Selected),
                    selectedMessagesIds.size()));
        }
    }

    private SpannableString addSpannable() {
        SpannableString spanString = new SpannableString(" ");
        Drawable d = getResources().getDrawable(R.drawable.mm_title_btn_phone);
        d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        ImageSpan span = new ImageSpan(d, ImageSpan.ALIGN_BASELINE);
        spanString.setSpan(span, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spanString;
    }

    private void updateSubtitle() {
        if (isFinish) {
            return;
        }

        if (paused || getActivity() == null) {
            return;
        }

        ActionBar actionBar = parentActivity.getSupportActionBar();

        TextView title = (TextView) parentActivity
                .findViewById(R.id.action_bar_title);
        if (title == null) {
            final int subtitleId = parentActivity.getResources().getIdentifier(
                    "action_bar_title", "id", "android");
            title = (TextView) parentActivity.findViewById(subtitleId);
        }

        if (currentChat != null) {

            String titleString = currentChat.title;
            if (StringUtil.isEmpty(titleString)) {
                titleString = getTitle();
            }
            actionBar.setTitle(titleString);
            if (title != null) {
                title.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                title.setCompoundDrawablePadding(0);
            }
        } else if (currentUser != null) {
            String nameTextString = Utilities.formatName(currentUser);
            if (Utilities.isPhone(UserConfig.account))// xueqiang change
            {
                if (currentUser.id / 1000 != 333
                        && ContactsController.getInstance().contactsDict
                        .get(currentUser.id) == null
                        && (ContactsController.getInstance().contactsDict
                        .size() != 0 || !ContactsController
                        .getInstance().loadingContacts)) {
                    // if (currentUser.phone != null &&
                    // currentUser.phone.length() != 0) {
                    // //actionBar.setTitle(PhoneFormat.getInstance().format("+"
                    // + currentUser.phone));
                    // actionBar.setTitle(PhoneFormat.getInstance().format(currentUser.phone));
                    // } else {
                    actionBar.setTitle(nameTextString);
                    // }
                } else {
                    actionBar.setTitle(nameTextString);
                }
            } else
                actionBar.setTitle(nameTextString);

            if (title != null) {
                if (currentEncryptedChat != null) {
                    title.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_lock_white, 0, 0, 0);
                    title.setCompoundDrawablePadding(Utilities.dp(4));
                } else {
                    title.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                    title.setCompoundDrawablePadding(0);
                }
            }

        }

        CharSequence printString = MessagesController.getInstance().printingStrings
                .get(dialog_id);
        if (printString == null || printString.length() == 0) {
            lastPrintString = null;
            setTypingAnimation(false);
            if (currentChat != null) {
                if (currentChat instanceof TLRPC.TL_chatForbidden) {
                    actionBar.setSubtitle(LocaleController.getString(
                            "YouWereKicked", R.string.YouWereKicked));
                } else if (currentChat.left) {
                    // actionBar.setSubtitle(LocaleController.getString("YouLeft",
                    // R.string.YouLeft));
                    actionBar.setSubtitle(LocaleController.getString(
                            "YouWereKicked", R.string.YouWereKicked));
                    bottomOverlayChat.setVisibility(View.VISIBLE);
                } else {
                    bottomOverlayChat.setVisibility(View.GONE);
                    if (onlineCount > 0 && currentChat.participants_count != 0) {
                        actionBar.setSubtitle(String.format("%d %s, %d %s",
                                currentChat.participants_count,
                                LocaleController.getString("Members",
                                        R.string.Members), onlineCount,
                                LocaleController.getString("Online",
                                        R.string.Online)));
                    } else {
                        actionBar.setSubtitle(String.format("%d %s",
                                currentChat.participants_count,
                                LocaleController.getString("Members",
                                        R.string.Members)));
                    }
                }
            } else if (currentUser != null) {
                if (currentUser.status == null) {
                    // actionBar.setSubtitle(getStringEntry(R.string.Offline));
                    // // jenf 去锟斤拷锟斤拷锟斤拷状态
                } else {
                    int currentTime = ConnectionsManager.getInstance()
                            .getCurrentTime();
                    if (currentUser.status.expires > currentTime) {
                        // actionBar.setSubtitle(LocaleController.getString("Online",
                        // R.string.Online));
                    } else {
                        if (currentUser.status.expires <= 10000) {
                            // actionBar.setSubtitle(LocaleController.getString("Invisible",
                            // R.string.Invisible));
                        } else {
                            // actionBar.setSubtitle(LocaleController.formatDateOnline(currentUser.status.expires));
                        }
                    }
                }
            }
        } else {
            lastPrintString = printString;
            actionBar.setSubtitle(printString);
            setTypingAnimation(true);
        }
        if (MessagesController.getInstance().earphone) {
            title.append(addSpannable());
        }
    }

    private String getTitle() {
        if (chatinfo == null) {
            return null;
        }
        StringBuffer stringBuffer = new StringBuffer();
        for (TLRPC.TL_chatParticipant participant : chatinfo.participants) {
            TLRPC.User user = MessagesController.getInstance().users.get(participant.user_id);
            if (user == null) {
                return null;
            }
            stringBuffer.append(user.last_name + user.first_name);
            stringBuffer.append(",");
        }
        return stringBuffer.toString();
    }

    private void checkAndUpdateAvatar() {
        TLRPC.FileLocation newPhoto = null;
        int placeHolderId = 0;
        if (currentUser != null) {
            currentUser = MessagesController.getInstance().users
                    .get(currentUser.id);
            if (currentUser.photo != null) {
                newPhoto = currentUser.photo.photo_small;
            }
            placeHolderId = Utilities.getUserAvatarForId(currentUser.id);
        } else if (currentChat != null) {
            //			currentChat = MessagesController.getInstance().chats
            //					.get(currentChat.id);
            if (currentChat.photo != null) {
                newPhoto = currentChat.photo.photo_small;
            }
            placeHolderId = Utilities.getGroupAvatarForId(currentChat.id);
        }
        if (avatarImageView != null) {
            avatarImageView.setImage(newPhoto, "50_50", placeHolderId);
        }
    }

    @Override
    public void onSizeChanged(int height) {
        Rect localRect = new Rect();
        parentActivity.getWindow().getDecorView()
                .getWindowVisibleDisplayFrame(localRect);

        WindowManager manager = (WindowManager) ApplicationLoader.applicationContext
                .getSystemService(Activity.WINDOW_SERVICE);
        if (manager == null || manager.getDefaultDisplay() == null) {
            return;
        }
        int rotation = manager.getDefaultDisplay().getRotation();

        if (height > Emoji.scale(50)) {
            if (rotation == Surface.ROTATION_270
                    || rotation == Surface.ROTATION_90) {
                keyboardHeightLand = height;
                parentActivity.getSharedPreferences("emoji", 0).edit()
                        .putInt("kbd_height_land3", keyboardHeightLand)
                        .commit();
            } else {
                keyboardHeight = height;
                parentActivity.getSharedPreferences("emoji", 0).edit()
                        .putInt("kbd_height", keyboardHeight).commit();
            }
        }

        if (emojiPopup != null && emojiPopup.isShowing()) {
            WindowManager wm = (WindowManager) parentActivity
                    .getSystemService(Context.WINDOW_SERVICE);

            if (Integer.valueOf(android.os.Build.VERSION.SDK) < 23) {


                final WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) emojiPopup
                        .getContentView().getLayoutParams();
                layoutParams.width = contentView.getWidth();
                if (rotation == Surface.ROTATION_270
                        || rotation == Surface.ROTATION_90) {
                    layoutParams.height = keyboardHeightLand;
                } else {
                    layoutParams.height = keyboardHeight;
                }
                wm.updateViewLayout(emojiPopup.getContentView(), layoutParams);
                if (!keyboardVisible) {
                    contentView.post(new Runnable() {
                        @Override
                        public void run() {
                            contentView.setPadding(0, 0, 0, layoutParams.height);
                            contentView.requestLayout();
                        }
                    });
                }
            } else {
                final FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) emojiPopup
                        .getContentView().getLayoutParams();
                layoutParams.width = contentView.getWidth();
                if (rotation == Surface.ROTATION_270
                        || rotation == Surface.ROTATION_90) {
                    layoutParams.height = keyboardHeightLand;
                } else {
                    layoutParams.height = keyboardHeight;
                }
                emojiPopup.getContentView().setLayoutParams(layoutParams);

                if (!keyboardVisible) {
                    contentView.post(new Runnable() {
                        @Override
                        public void run() {
                            contentView.setPadding(0, 0, 0, layoutParams.height);
                            contentView.requestLayout();
                        }
                    });
                }
            }

        }

        boolean oldValue = keyboardVisible;
        keyboardVisible = height > 0;
        if (keyboardVisible && contentView.getPaddingBottom() > 0) {
            showEmojiPopup(false);
        } else if (!keyboardVisible && keyboardVisible != oldValue
                && emojiPopup != null && emojiPopup.isShowing()) {
            showEmojiPopup(false);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == 100) {
                //callViewSet();
            } else if (requestCode == 0) {
                Utilities.addMediaToGallery(currentPicturePath);
                processSendingPhoto(currentPicturePath, null);
                currentPicturePath = null;
            } else if (requestCode == 1) {
                if (data == null) {
                    return;
                }
                //锟矫伙拷选锟斤拷锟斤拷一锟斤拷图片锟斤拷使锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟酵计�
                processSendingPhoto(null, data.getData());
            } else if (requestCode == 2) {
                String videoPath = null;
                if (data != null) {
                    Uri uri = data.getData();
                    boolean fromCamera = false;
                    if (uri != null && uri.getScheme() != null) {
                        fromCamera = uri.getScheme().contains("file");
                    } else if (uri == null) {
                        fromCamera = true;
                    }
                    if (fromCamera) {
                        if (uri != null) {
                            videoPath = uri.getPath();
                        } else {
                            videoPath = currentPicturePath;
                        }
                        Utilities.addMediaToGallery(currentPicturePath);
                        currentPicturePath = null;
                    } else {
                        try {
                            videoPath = Utilities.getPath(uri);
                        } catch (Exception e) {
                            FileLog.e("emm", e);
                        }
                    }
                }
                if (videoPath == null && currentPicturePath != null) {
                    File f = new File(currentPicturePath);
                    if (f.exists()) {
                        videoPath = currentPicturePath;
                    }
                    currentPicturePath = null;
                }
                processSendingVideo(videoPath);
            }

        }
    }

    public boolean processSendingText(String text) {
        text = text.replaceAll("\n\n+", "\n\n");
        text = text.replaceAll(" +", " ");
        if (text.length() != 0) {
            int count = (int) Math.ceil(text.length() / 2048.0f);
            for (int a = 0; a < count; a++) {
                String mess = text.substring(a * 2048,
                        Math.min((a + 1) * 2048, text.length()));
                this.showConnectStatus();
                MessagesController.getInstance().sendMessage(mess, dialog_id);
            }
            return true;
        }
        return false;
    }

    public void processSendingPhoto(String imageFilePath, Uri imageUri) {
        if ((imageFilePath == null || imageFilePath.length() == 0)
                && imageUri == null) {
            return;
        }
        if (imageUri != null)
            FileLog.e(
                    "emm",
                    "image hashcode=" + imageUri.hashCode()
                            + imageUri.getPath());
        TLRPC.TL_photo photo = MessagesController.getInstance().generatePhotoSizes(imageFilePath, imageUri);
        if (photo != null) {
            // xueqiang add
            showConnectStatus();

            MessagesController.getInstance().sendMessage(photo, dialog_id);
            if (chatListView != null) {
                chatListView.setSelection(messages.size() + 1);
            }
            scrollToTopOnResume = true;
        }
    }

    public void processSendingDocument(String documentFilePath) {
        if (documentFilePath == null || documentFilePath.length() == 0) {
            return;
        }
        File f = new File(documentFilePath);
        if (!f.exists() || f.length() == 0) {
            return;
        }
        String name = f.getName();
        if (name == null) {
            name = "noname";
        }
        String ext = "";
        int idx = documentFilePath.lastIndexOf(".");
        if (idx != -1) {
            ext = documentFilePath.substring(idx);
        }
        TLRPC.TL_document document = new TLRPC.TL_document();
        document.thumb = new TLRPC.TL_photoSizeEmpty();
        document.thumb.type = "s";
        document.id = 0;
        document.user_id = UserConfig.clientUserId;
        document.date = ConnectionsManager.getInstance().getCurrentTime();
        document.file_name = name;
        document.size = (int) f.length();
        document.dc_id = 0;
        document.path = documentFilePath;
        if (ext.length() != 0) {
            MimeTypeMap myMime = MimeTypeMap.getSingleton();
            String mimeType = myMime
                    .getMimeTypeFromExtension(ext.toLowerCase());
            if (mimeType != null) {
                document.mime_type = mimeType;
            } else {
                document.mime_type = "application/octet-stream";
            }
        } else {
            document.mime_type = "application/octet-stream";
        }
        MessagesController.getInstance().sendMessage(document, dialog_id);
    }

    public void processSendingVideo(final String videoPath) {
        if (videoPath == null || videoPath.length() == 0) {
            return;
        }
        Bitmap thumb = ThumbnailUtils.createVideoThumbnail(videoPath,
                MediaStore.Video.Thumbnails.MINI_KIND);
        TLRPC.PhotoSize size = FileLoader.scaleAndSaveImage(thumb, 90, 90, 55,
                currentEncryptedChat != null);
        if (size == null) {
            return;
        }
        size.type = "s";
        TLRPC.TL_video video = new TLRPC.TL_video();
        video.thumb = size;
        video.caption = "";
        video.id = 0;
        video.path = videoPath;
        File temp = new File(videoPath);
        if (temp != null && temp.exists()) {
            video.size = (int) temp.length();
        }
        UserConfig.lastLocalId--;
        UserConfig.saveConfig(false);

        MediaPlayer mp = MediaPlayer.create(
                ApplicationLoader.applicationContext,
                Uri.fromFile(new File(videoPath)));
        if (mp == null) {
            return;
        }
        video.duration = (int) Math.ceil(mp.getDuration() / 1000.0f);
        video.w = mp.getVideoWidth();
        video.h = mp.getVideoHeight();
        mp.release();

        MediaStore.Video.Media media = new MediaStore.Video.Media();
        MessagesController.getInstance().sendMessage(video, dialog_id);
        if (chatListView != null) {
            chatListView.setSelection(messages.size() + 1);
        }
        scrollToTopOnResume = true;
    }

    private void removeUnreadPlane(boolean reload) {
        if (unreadMessageObject != null) {
            messages.remove(unreadMessageObject);
            unread_end_reached = true;
            first_unread_id = 0;
            last_unread_id = 0;
            unread_to_load = 0;
            unreadMessageObject = null;
            if (reload) {
                chatAdapter.notifyDataSetChanged();
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void didReceivedNotification(int id, final Object... args) {
        if (id == MessagesController.messagesDidLoaded) {
            FileLog.e("emm", "load messages end");
            long did = (Long) args[0];
            if (did == dialog_id) {
                int offset = (Integer) args[1];
                int count = (Integer) args[2];
                boolean isCache = (Boolean) args[4];
                int fnid = (Integer) args[5];
                int last_unread_date = (Integer) args[8];
                boolean forwardLoad = (Boolean) args[9];
                boolean wasUnread = false;
                boolean positionToUnread = false;
                if (fnid != 0) {
                    first_unread_id = (Integer) args[5];
                    last_unread_id = (Integer) args[6];
                    unread_to_load = (Integer) args[7];
                    positionToUnread = true;
                }
                ArrayList<MessageObject> messArr = (ArrayList<MessageObject>) args[3];

                int newRowsCount = 0;
                unread_end_reached = last_unread_id == 0;
                for (int a = 0; a < messArr.size(); a++) {
                    MessageObject obj = messArr.get(a);
                    if (messagesDict.containsKey(obj.messageOwner.id)) {
                        continue;
                    }

                    if (obj.messageOwner.id > 0) {
                        maxMessageId = Math.min(obj.messageOwner.id,
                                maxMessageId);
                        minMessageId = Math.max(obj.messageOwner.id,
                                minMessageId);
                    } else if (currentEncryptedChat != null) {
                        maxMessageId = Math.max(obj.messageOwner.id,
                                maxMessageId);
                        minMessageId = Math.min(obj.messageOwner.id,
                                minMessageId);
                    }
                    maxDate = Math.max(maxDate, obj.messageOwner.date);
                    if (minDate == 0 || obj.messageOwner.date < minDate) {
                        minDate = obj.messageOwner.date;
                    }
                    if (!obj.messageOwner.out && obj.messageOwner.unread) {
                        wasUnread = true;
                    }
                    messagesDict.put(obj.messageOwner.id, obj);
                    ArrayList<MessageObject> dayArray = messagesByDays
                            .get(obj.dateKey);

                    if (dayArray == null) {
                        dayArray = new ArrayList<MessageObject>();
                        messagesByDays.put(obj.dateKey, dayArray);

                        TLRPC.Message dateMsg = new TLRPC.Message();

                        // wangxm repair 锟睫革拷锟斤拷china状态锟斤拷 锟斤拷示时锟斤拷锟斤拷锟绞�
                        if (locale_language.equals("CN")) {
                            dateMsg.message = DateUnit
                                    .getMMddFormat(obj.messageOwner.date);
                        } else {
                            dateMsg.message = LocaleController
                                    .formatDateChat(obj.messageOwner.date);
                        }

                        // dateMsg.message =
                        // LocaleController.formatDateChat(obj.messageOwner.date);
                        dateMsg.id = 0;
                        MessageObject dateObj = new MessageObject(dateMsg, null);
                        dateObj.type = 10;
                        if (forwardLoad) {
                            messages.add(0, dateObj);
                        } else {
                            messages.add(dateObj);
                        }
                        newRowsCount++;
                    }

                    newRowsCount++;
                    dayArray.add(obj);
                    if (forwardLoad) {
                        messages.add(0, obj);
                    } else {
                        messages.add(messages.size() - 1, obj);
                    }

                    if (!forwardLoad) {
                        if (obj.messageOwner.id == first_unread_id) {
                            TLRPC.Message dateMsg = new TLRPC.Message();
                            dateMsg.message = "";
                            dateMsg.id = 0;
                            MessageObject dateObj = new MessageObject(dateMsg,
                                    null);
                            dateObj.type = 15;
                            boolean dateAdded = true;
                            if (a != messArr.size() - 1) {
                                MessageObject next = messArr.get(a + 1);
                                dateAdded = !next.dateKey.equals(obj.dateKey);
                            }
                            messages.add(messages.size() - (dateAdded ? 0 : 1),
                                    dateObj);
                            unreadMessageObject = dateObj;
                            newRowsCount++;
                        }
                        if (obj.messageOwner.id == last_unread_id) {
                            unread_end_reached = true;
                        }
                    }

                }

                if (unread_end_reached) {
                    first_unread_id = 0;
                    last_unread_id = 0;
                }

                if (forwardLoad) {
                    if (messArr.size() != count) {
                        unread_end_reached = true;
                        first_unread_id = 0;
                        last_unread_id = 0;
                    }

                    chatAdapter.notifyDataSetChanged();
                    loadingForward = false;
                } else {
                    if (messArr.size() != count) {
                        if (isCache) {
                            cacheEndReaced = true;
                            if (currentEncryptedChat != null) {
                                endReached = true;
                            }
                        } else {
                            cacheEndReaced = true;
                            endReached = true;
                        }
                    }
                    loading = false;

                    if (chatListView != null) {
                        if (first || scrollToTopOnResume) {
                            chatAdapter.notifyDataSetChanged();
                            if (positionToUnread && unreadMessageObject != null) {
                                if (messages.get(messages.size() - 1) == unreadMessageObject) {
                                    chatListView.setSelectionFromTop(0,
                                            Utilities.dp(-11));
                                } else {
                                    chatListView
                                            .setSelectionFromTop(
                                                    messages.size()
                                                            - messages
                                                            .indexOf(unreadMessageObject),
                                                    Utilities.dp(-11));
                                }
                                ViewTreeObserver obs = chatListView
                                        .getViewTreeObserver();
                                obs.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                                    @Override
                                    public boolean onPreDraw() {
                                        if (messages.get(messages.size() - 1) == unreadMessageObject) {
                                            chatListView.setSelectionFromTop(0,
                                                    Utilities.dp(-11));
                                        } else {
                                            chatListView.setSelectionFromTop(
                                                    messages.size()
                                                            - messages
                                                            .indexOf(unreadMessageObject),
                                                    Utilities.dp(-11));
                                        }
                                        chatListView.getViewTreeObserver()
                                                .removeOnPreDrawListener(this);
                                        return false;
                                    }
                                });
                                chatListView.invalidate();
                                showPagedownButton(true, true);
                            } else {
                                chatListView.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        chatListView.setSelectionFromTop(
                                                messages.size() - 1,
                                                -100000
                                                        - chatListView
                                                        .getPaddingTop());
                                    }
                                });
                            }
                        } else {
                            int firstVisPos = chatListView
                                    .getLastVisiblePosition();
                            View firstVisView = chatListView
                                    .getChildAt(chatListView.getChildCount() - 1);
                            int top = ((firstVisView == null) ? 0
                                    : firstVisView.getTop())
                                    - chatListView.getPaddingTop();
                            chatAdapter.notifyDataSetChanged();
                            chatListView.setSelectionFromTop(firstVisPos
                                    + newRowsCount, top);
                        }

                        if (paused) {
                            scrollToTopOnResume = true;
                            if (positionToUnread && unreadMessageObject != null) {
                                scrollToTopUnReadOnResume = true;
                            }
                        }

                        if (first) {
                            if (chatListView.getEmptyView() == null) {
                                if (currentEncryptedChat == null) {
                                    chatListView.setEmptyView(emptyView);
                                } else {
                                    chatListView
                                            .setEmptyView(secretChatPlaceholder);
                                }
                            }
                        }
                    } else {
                        scrollToTopOnResume = true;
                        if (positionToUnread && unreadMessageObject != null) {
                            scrollToTopUnReadOnResume = true;
                        }
                    }
                }

                if (first && messages.size() > 0) {
                    if (last_unread_id != 0) {
                        MessagesController.getInstance().markDialogAsRead(
                                dialog_id, messages.get(0).messageOwner.id,
                                last_unread_id, 0, last_unread_date, wasUnread);
                    } else {
                        MessagesController.getInstance().markDialogAsRead(
                                dialog_id, messages.get(0).messageOwner.id,
                                minMessageId, 0, maxDate, wasUnread);
                    }
                    first = false;
                }

                if (progressView != null) {
                    progressView.setVisibility(View.GONE);
                }
            }
        } else if (id == 999) {
            if (animationInProgress) {
                invalidateAfterAnimation = true;
            } else {
                if (chatListView != null) {
                    chatListView.invalidateViews();
                }
            }
            if (emojiView != null) {
                emojiView.invalidateViews();
            }
        } else if (id == MessagesController.updateInterfaces) {
            int updateMask = (Integer) args[0];
            if ((updateMask & MessagesController.UPDATE_MASK_NAME) != 0
                    || (updateMask & MessagesController.UPDATE_MASK_STATUS) != 0
                    || (updateMask & MessagesController.UPDATE_MASK_CHAT_NAME) != 0
                    || (updateMask & MessagesController.UPDATE_MASK_CHAT_MEMBERS) != 0) {
                updateSubtitle();
                updateOnlineCount();
            }
            if ((updateMask & MessagesController.UPDATE_MASK_AVATAR) != 0
                    || (updateMask & MessagesController.UPDATE_MASK_CHAT_AVATAR) != 0
                    || (updateMask & MessagesController.UPDATE_MASK_NAME) != 0) {
                checkAndUpdateAvatar();
                if (animationInProgress) {
                    invalidateAfterAnimation = true;
                } else {
                    if (chatListView != null) {
                        updateVisibleRows();
                    }
                }
            }
            if ((updateMask & MessagesController.UPDATE_MASK_USER_PRINT) != 0) {
                CharSequence printString = MessagesController.getInstance().printingStrings
                        .get(dialog_id);
                if (lastPrintString != null && printString == null
                        || lastPrintString == null && printString != null
                        || lastPrintString != null && printString != null
                        && !lastPrintString.equals(printString)) {
                    updateSubtitle();
                }
            }
            if ((updateMask & MessagesController.UPDATE_MASK_USER_PHONE) != 0) {
                updateContactStatus();
            }
        } else if (id == MessagesController.didReceivedNewMessages) {
            long did = (Long) args[0];
            if (did == dialog_id) {

                boolean updateChat = false;
                ArrayList<MessageObject> arr = (ArrayList<MessageObject>) args[1];

                if (!unread_end_reached) {
                    int currentMaxDate = Integer.MIN_VALUE;
                    int currentMinMsgId = Integer.MIN_VALUE;
                    if (currentEncryptedChat != null) {
                        currentMinMsgId = Integer.MAX_VALUE;
                    }
                    boolean currentMarkAsRead = false;

                    for (MessageObject obj : arr) {
                        if (currentEncryptedChat != null
                                && obj.messageOwner.action != null
                                && obj.messageOwner.action instanceof TLRPC.TL_messageActionTTLChange
                                && timerButton != null) {
                            timerButton.setTime(obj.messageOwner.action.ttl);
                        }
                        if (obj.messageOwner.out
                                && obj.messageOwner.send_state == MessagesController.MESSAGE_SEND_STATE_SENDING) {
                            scrollToLastMessage();
                            return;
                        }
                        if (messagesDict.containsKey(obj.messageOwner.id)) {
                            continue;
                        }
                        currentMaxDate = Math.max(currentMaxDate,
                                obj.messageOwner.date);
                        if (obj.messageOwner.id > 0) {
                            currentMinMsgId = Math.max(obj.messageOwner.id,
                                    currentMinMsgId);
                        } else if (currentEncryptedChat != null) {
                            currentMinMsgId = Math.min(obj.messageOwner.id,
                                    currentMinMsgId);
                        }

                        if (!obj.messageOwner.out && obj.messageOwner.unread) {
                            unread_to_load++;
                            currentMarkAsRead = true;
                        }
                        if (obj.type == 10 || obj.type == 11) {
                            updateChat = true;
                        }
                    }

                    if (currentMarkAsRead) {
                        if (paused) {
                            readWhenResume = true;
                            readWithDate = currentMaxDate;
                            readWithMid = currentMinMsgId;
                        } else {
                            if (messages.size() > 0) {
                                MessagesController
                                        .getInstance()
                                        .markDialogAsRead(
                                                dialog_id,
                                                messages.get(0).messageOwner.id,
                                                currentMinMsgId, 0,
                                                currentMaxDate, true);
                            }
                        }
                    }
                    updateVisibleRows();
                } else {
                    boolean markAsRead = false;
                    int oldCount = messages.size();
                    for (MessageObject obj : arr) {
                        if (currentEncryptedChat != null
                                && obj.messageOwner.action != null
                                && obj.messageOwner.action instanceof TLRPC.TL_messageActionTTLChange
                                && timerButton != null) {
                            timerButton.setTime(obj.messageOwner.action.ttl);
                        }
                        if (messagesDict.containsKey(obj.messageOwner.id)) {
                            continue;
                        }
                        if (minDate == 0 || obj.messageOwner.date < minDate) {
                            minDate = obj.messageOwner.date;
                        }
                        if (obj.messageOwner.attachPath != null
                                && obj.messageOwner.attachPath.length() != 0) {
                            progressBarMap.put(obj.messageOwner.attachPath,
                                    null);
                        }

                        if (obj.messageOwner.out) {
                            removeUnreadPlane(false);
                        }

                        if (!obj.messageOwner.out
                                && unreadMessageObject != null) {
                            unread_to_load++;
                        }

                        if (obj.messageOwner.id > 0) {
                            maxMessageId = Math.min(obj.messageOwner.id,
                                    maxMessageId);
                            minMessageId = Math.max(obj.messageOwner.id,
                                    minMessageId);
                        } else if (currentEncryptedChat != null) {
                            maxMessageId = Math.max(obj.messageOwner.id,
                                    maxMessageId);
                            minMessageId = Math.min(obj.messageOwner.id,
                                    minMessageId);
                        }
                        maxDate = Math.max(maxDate, obj.messageOwner.date);
                        //锟斤拷锟斤拷锟节凤拷锟酵碉拷锟斤拷息锟芥储锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷晒锟斤拷螅锟斤拷锟秸碉拷messageReceivedByServer锟斤拷息
                        //通锟斤拷锟斤拷锟绞憋拷锟斤拷id为锟斤拷锟斤拷
                        messagesDict.put(obj.messageOwner.id, obj);
                        ArrayList<MessageObject> dayArray = messagesByDays
                                .get(obj.dateKey);
                        if (dayArray == null) {
                            dayArray = new ArrayList<MessageObject>();
                            messagesByDays.put(obj.dateKey, dayArray);

                            TLRPC.Message dateMsg = new TLRPC.Message();
                            // wangxm repair
                            if (locale_language.equals("CN")) {
                                dateMsg.message = DateUnit
                                        .getMMddFormat(obj.messageOwner.date);
                            } else {
                                dateMsg.message = LocaleController
                                        .formatDateChat(obj.messageOwner.date);
                            }
                            // dateMsg.message =
                            // LocaleController.formatDateChat(obj.messageOwner.date);
                            dateMsg.id = 0;
                            MessageObject dateObj = new MessageObject(dateMsg,
                                    null);
                            dateObj.type = 10;
                            messages.add(0, dateObj);
                        }
                        if (!obj.messageOwner.out && obj.messageOwner.unread) {
                            obj.messageOwner.unread = false;
                            markAsRead = true;
                        }
                        dayArray.add(0, obj);
                        messages.add(0, obj);
                        Log.e("emm", "getview msg=" + obj.messageOwner.message);
                        if (obj.type == 10 || obj.type == 11) {
                            updateChat = true;
                        }
                    }
                    if (progressView != null) {
                        progressView.setVisibility(View.GONE);
                    }
                    if (chatAdapter != null) {
                        chatAdapter.notifyDataSetChanged();
                    } else {
                        scrollToTopOnResume = true;
                    }

                    if (chatListView != null && chatAdapter != null) {
                        int lastVisible = chatListView.getLastVisiblePosition();
                        if (endReached) {
                            lastVisible++;
                        }
                        if (lastVisible == oldCount) {
                            if (paused) {
                                scrollToTopOnResume = true;
                            } else {
                                chatListView.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        chatListView.setSelectionFromTop(
                                                messages.size() - 1,
                                                -100000
                                                        - chatListView
                                                        .getPaddingTop());
                                    }
                                });
                            }
                        } else {
                            showPagedownButton(true, true);
                        }
                    } else {
                        scrollToTopOnResume = true;
                    }

                    if (markAsRead) {
                        if (paused) {
                            readWhenResume = true;
                            readWithDate = maxDate;
                            readWithMid = minMessageId;
                        } else {
                            MessagesController.getInstance().markDialogAsRead(
                                    dialog_id, messages.get(0).messageOwner.id,
                                    minMessageId, 0, maxDate, true);
                        }
                    }
                }
                if (updateChat) {
                    updateSubtitle();
                    checkAndUpdateAvatar();
                }
//				//xiaoyang
//				Intent intent=new Intent();
//				intent.setAction("MeetingMsg");
//				intent.putExtra("TXT_MSG",arr.get(0).messageText.toString() );//通锟斤拷锟姐播锟斤拷锟斤拷锟斤拷锟斤拷
//				getActivity().sendBroadcast(intent);
            }
        } else if (id == MessagesController.closeChats) {
            if (messsageEditText != null && messsageEditText.isFocused()) {
                Utilities.hideKeyboard(messsageEditText);
            }
            removeSelfFromStack();
        } else if (id == MessagesController.messagesReaded) {
            ArrayList<Integer> markAsReadMessages = (ArrayList<Integer>) args[0];
            boolean updated = false;
            for (Integer ids : markAsReadMessages) {
                MessageObject obj = messagesDict.get(ids);
                if (obj != null) {
                    obj.messageOwner.unread = false;
                    updated = true;
                }
            }
            if (updated) {
                if (animationInProgress) {
                    invalidateAfterAnimation = true;
                } else {
                    if (chatListView != null) {
                        updateVisibleRows();
                    }
                }
            }
        } else if (id == MessagesController.messagesDeleted) {
            ArrayList<Integer> markAsDeletedMessages = (ArrayList<Integer>) args[0];
            boolean bDeleteFile = (Boolean) args[1];
            boolean updated = false;
            for (Integer ids : markAsDeletedMessages) {
                MessageObject obj = messagesDict.get(ids);
                if (obj != null) {
                    int index = messages.indexOf(obj);
                    if (index != -1) {
                        messages.remove(index);
                        messagesDict.remove(ids);
                        ArrayList<MessageObject> dayArr = messagesByDays
                                .get(obj.dateKey);
                        dayArr.remove(obj);
                        if (dayArr.isEmpty()) {
                            messagesByDays.remove(obj.dateKey);
                            if (index != -1) {
                                messages.remove(index);
                            }
                        }
                        updated = true;
                    }
                    // 删锟斤拷锟斤拷锟斤拷锟斤拷息
                    if (obj.messageOwner.media != null
                            && obj.messageOwner.media instanceof TLRPC.TL_messageMediaAudio
                            && bDeleteFile) {
                        // 锟斤拷系统目录锟斤拷删锟斤拷锟斤拷锟斤拷锟侥硷拷 xueqiang change
                        TLRPC.Audio audio = obj.messageOwner.media.audio;
                        final File cacheFile = new File(
                                Utilities.getSystemDir(),
                                MessageObject.getAttachFileName(audio));
                        if (cacheFile != null && cacheFile.exists()) {
                            cacheFile.delete();
                            //							FileLog.e("emm", "delete audio file");
                        }
                    }
                }
            }
            if (messages.isEmpty()) {
                if (!endReached && !loading) {
                    progressView.setVisibility(View.GONE);
                    chatListView.setEmptyView(null);
                    if (currentEncryptedChat == null) {
                        maxMessageId = Integer.MAX_VALUE;
                        minMessageId = Integer.MIN_VALUE;
                    } else {
                        maxMessageId = Integer.MIN_VALUE;
                        minMessageId = Integer.MAX_VALUE;
                    }
                    maxDate = Integer.MIN_VALUE;
                    minDate = 0;
                    MessagesController.getInstance().loadMessages(dialog_id, 0,
                            30, 0, !cacheEndReaced, minDate, classGuid, false,
                            false);
                    loading = true;
                }
            }
            if (updated && chatAdapter != null) {
                removeUnreadPlane(false);
                chatAdapter.notifyDataSetChanged();
            }
        } else if (id == MessagesController.messageReceivedByServer) {
            Integer msgId = (Integer) args[0];
            //xueqiang add 锟斤拷息锟斤拷锟斤拷为obj.type=20锟斤拷锟斤拷示锟揭凤拷锟酵碉拷alert锟斤拷锟斤拷息锟斤拷为obj.type=21锟斤拷示锟揭斤拷锟秸碉拷alert锟斤拷息
            //锟斤拷锟斤拷锟斤拷息锟斤拷锟斤拷锟斤拷要锟斤拷锟斤拷一锟斤拷锟铰碉拷cell,也锟斤拷锟斤拷view锟斤拷示锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟�
            MessageObject obj = messagesDict.get(msgId);
            if (obj != null) {
                Integer newMsgId = (Integer) args[1];
                MessageObject newMsgObj = (MessageObject) args[2];
                if (newMsgObj != null) {
                    obj.messageOwner.media = newMsgObj.messageOwner.media;
                    if (obj.type == 20)//xueqiang change for scroll alert msg
                        scrollToLastMessage();
                }
                messagesDict.remove(msgId);
                messagesDict.put(newMsgId, obj);
                obj.messageOwner.id = newMsgId;
                obj.messageOwner.send_state = MessagesController.MESSAGE_SEND_STATE_SENT;
                if (animationInProgress) {
                    invalidateAfterAnimation = true;
                } else {
                    if (chatListView != null) {
                        updateVisibleRows();
                    }
                }
                if (obj.messageOwner.attachPath != null
                        && obj.messageOwner.attachPath.length() != 0) {
                    progressBarMap.remove(obj.messageOwner.attachPath);
                }
            }
        } else if (id == MessagesController.messageReceivedByAck) {
            Integer msgId = (Integer) args[0];
            MessageObject obj = messagesDict.get(msgId);
            if (obj != null) {
                if (obj.messageOwner.attachPath != null
                        && obj.messageOwner.attachPath.length() != 0) {
                    progressBarMap.remove(obj.messageOwner.attachPath);
                }
                obj.messageOwner.send_state = MessagesController.MESSAGE_SEND_STATE_SENT;
                if (animationInProgress) {
                    invalidateAfterAnimation = true;
                } else {
                    if (chatListView != null) {
                        updateVisibleRows();
                    }
                }
            }
        } else if (id == MessagesController.messageSendError) {
            Integer msgId = (Integer) args[0];
            MessageObject obj = messagesDict.get(msgId);
            if (obj != null) {
                obj.messageOwner.send_state = MessagesController.MESSAGE_SEND_STATE_SEND_ERROR;
                if (animationInProgress) {
                    invalidateAfterAnimation = true;
                } else {
                    if (chatListView != null) {
                        updateVisibleRows();
                    }
                }
                if (obj.messageOwner.attachPath != null
                        && obj.messageOwner.attachPath.length() != 0) {
                    progressBarMap.remove(obj.messageOwner.attachPath);
                }
            }
        } else if (id == 997) {
            MessagesController.getInstance().sendMessage((Double) args[0],
                    (Double) args[1], dialog_id);
            if (chatListView != null) {
                chatListView.setSelection(messages.size() + 1);
                scrollToTopOnResume = true;
            }
        } else if (id == MessagesController.chatInfoDidLoaded) {
            int chatId = (Integer) args[0];
            if (currentChat != null && chatId == currentChat.id) {
                chatinfo = (TLRPC.ChatParticipants) args[1];
                TLRPC.Chat chat = MessagesController.getInstance().chats
                        .get(chatId);
                if (chat != null && chatinfo != null) {
                    chat.participants_count = chatinfo.participants.size();
                    updateOnlineCount();
                }
            }
        } else if (id == FileLoader.FileUploadProgressChanged) {
            String location = (String) args[0];
            boolean enc = (Boolean) args[2];
            if (enc && currentEncryptedChat == null) {
                return;
            } else if (!enc && currentEncryptedChat != null) {
                return;
            }
            ProgressBar bar;
            if ((bar = progressBarMap.get(location)) != null) {
                Float progress = (Float) args[1];
                bar.setProgress((int) (progress * 100));
            }
        } else if (id == FileLoader.FileDidFailedLoad) {
            String location = (String) args[0];
            if (loadingFile.containsKey(location)) {
                loadingFile.remove(location);
                if (animationInProgress) {
                    invalidateAfterAnimation = true;
                } else {
                    if (chatListView != null) {
                        updateVisibleRows();
                    }
                }
            }
        } else if (id == FileLoader.FileDidLoaded) {
            String location = (String) args[0];
            if (loadingFile.containsKey(location)) {
                loadingFile.remove(location);
                if (animationInProgress) {
                    invalidateAfterAnimation = true;
                } else {
                    if (chatListView != null) {
                        updateVisibleRows();
                    }
                }
            }
        } else if (id == FileLoader.FileLoadProgressChanged) {
            String location = (String) args[0];
            ArrayList<ProgressBar> arr = loadingFile.get(location);
            if (arr != null) {
                Float progress = (Float) args[1];
                for (ProgressBar bar : arr) {
                    bar.setProgress((int) (progress * 100));
                }
            }
        } else if (id == MessagesController.contactsDidLoaded) {
            updateContactStatus();
            updateSubtitle();
        } else if (id == MessagesController.encryptedChatUpdated) {
            TLRPC.EncryptedChat chat = (TLRPC.EncryptedChat) args[0];
            if (currentEncryptedChat != null
                    && chat.id == currentEncryptedChat.id) {
                currentEncryptedChat = chat;
                updateContactStatus();
                updateSecretStatus();
            }
        } else if (id == MessagesController.messagesReadedEncrypted) {
            int encId = (Integer) args[0];
            if (currentEncryptedChat != null
                    && currentEncryptedChat.id == encId) {
                int date = (Integer) args[1];
                boolean started = false;
                for (MessageObject obj : messages) {
                    if (!obj.messageOwner.out) {
                        continue;
                    } else if (obj.messageOwner.out && !obj.messageOwner.unread) {
                        break;
                    }
                    if (obj.messageOwner.date <= date) {
                        obj.messageOwner.unread = false;
                    }
                }
                if (chatListView != null) {
                    updateVisibleRows();
                }
            }
        } else if (id == MediaController.audioDidReset) {
            Integer mid = (Integer) args[0];
            //			FileLog.e("emm", "handler audioDidReset msg mid=" + mid);
            if (chatListView != null) {
                int count = chatListView.getChildCount();
                for (int a = 0; a < count; a++) {
                    View view = chatListView.getChildAt(a);
                    if (view instanceof ChatAudioCell) {
                        ChatAudioCell cell = (ChatAudioCell) view;
                        if (cell.getMessageObject() != null
                                && cell.getMessageObject().messageOwner.id == mid) {
                            cell.getMessageObject().messageOwner.isPlaying = false;
                            cell.updateButtonState();
                            break;
                        }
                    }
                }
            }

        } else if (id == MediaController.audioProgressDidChanged) {
            Integer mid = (Integer) args[0];
            if (chatListView != null) {
                int count = chatListView.getChildCount();
                for (int a = 0; a < count; a++) {
                    View view = chatListView.getChildAt(a);
                    if (view instanceof ChatAudioCell) {
                        ChatAudioCell cell = (ChatAudioCell) view;
                        if (cell.getMessageObject() != null
                                && cell.getMessageObject().messageOwner.id == mid) {
                            cell.updateProgress();
                            break;
                        }
                    }
                }
            }
        } else if (id == MediaController.recordProgressChanged) {
            Long time = (Long) args[0] / 1000;
            String str = String.format("%02d:%02d", time / 60, time % 60);
            if (lastTimeString == null || !lastPrintString.equals(str)) {
                if (recordTimeText != null) {
                    recordTimeText.setText(str);
                }
            }
        } else if (id == MessagesController.removeAllMessagesFromDialog) {
            messages.clear();
            messagesByDays.clear();
            messagesDict.clear();
            progressView.setVisibility(View.GONE);
            if (currentEncryptedChat == null) {
                chatListView.setEmptyView(emptyView);
            } else {
                chatListView.setEmptyView(secretChatPlaceholder);
            }
            if (currentEncryptedChat == null) {
                maxMessageId = Integer.MAX_VALUE;
                minMessageId = Integer.MIN_VALUE;
            } else {
                maxMessageId = Integer.MIN_VALUE;
                minMessageId = Integer.MAX_VALUE;
            }
            maxDate = Integer.MIN_VALUE;
            minDate = 0;
            chatAdapter.notifyDataSetChanged();
            if (mActionMode != null) {
                mActionMode.finish();
                mActionMode = null;
            }
        } else if (id == MediaController.recordStartError
                || id == MediaController.recordStopped) {
            if (recordingAudio) {
                recordingAudio = false;
                updateAudioRecordIntefrace();
            }
        } else if (id == MediaController.recordStarted) {
            if (!recordingAudio) {
                recordingAudio = true;
                updateAudioRecordIntefrace();
            }
        } else if (id == MediaController.startplayaudio) {
            Integer mid = (Integer) args[0];
            for (int i = messages.size() - 1; i >= 0; i--) {
                MessageObject obj = messages.get(i);
                if (obj.messageOwner.out)
                    continue;
                if (obj.messageOwner.id == mid) {
                    chatAdapter.notifyDataSetChanged();
                    break;
                }
            }
            mProximitySensorListener.clearPendingRequests();
            enableProximitySensor();
        } else if (id == MediaController.playaudiocomplete) {
            Integer mid = (Integer) args[0];
            //			FileLog.e("emm", "play completed mid" + mid);
            Integer orgReadStatus = (Integer) args[1];
            //			if (orgReadStatus == 1)
            //				FileLog.e("emm", "chatactivity orgreadstatus=true");
            //			else
            //				FileLog.e("emm", "chatactivity orgreadstatus=false");
            // 锟斤拷锟斤拷锟斤拷锟斤拷丫锟斤拷锟斤拷锟斤拷模锟斤拷锟斤拷锟揭拷远锟斤拷锟斤拷锟斤拷锟揭伙拷锟斤拷锟侥�
            if (orgReadStatus == 1) {
                disableProximitySensor(false);
                mProximitySensorListener.clearPendingRequests();
                mProximitySensorListener.postDelayed(
                        mProximitySensorListener.mUnblankRunnable,
                        PROXIMITY_UNBLANK_DELAY_MILLIS);
                return;
            }
            onPlayNextAudio(mid);
        } else if (id == MessagesController.meeting_notice_bar) {
            callViewSet();
        } else if (id == MessagesController.alermDidLoaded) {
            RemindActivity fragment = new RemindActivity();
            Bundle bundle = new Bundle();
            bundle.putInt("userid", selectMessage.messageOwner.from_id);
            bundle.putLong("dialogid", dialog_id);

            if (args[1] != null && ((ArrayList<TLRPC.TL_alertMedia>) args[1]).get(0) != null) {
                TLRPC.TL_alertMedia alert = ((ArrayList<TLRPC.TL_alertMedia>) args[1]).get(0);
                bundle.putString("msg", alert.msg);
                bundle.putInt("date", alert.date);
                bundle.putInt("status", alert.status);
                bundle.putString("guid", alert.guid);
                bundle.putInt("lastModifyTime", alert.lastModifyTime);
                bundle.putInt("id", alert.id);//xueqiang change for scheduleAlert使锟矫的ｏ拷锟斤拷锟诫传锟斤拷reminder
                fragment.setArguments(bundle);
            }

            ((LaunchActivity) parentActivity).presentFragment(fragment,
                    "remind", false);
        } else if (MeetingMgr.CHECK_MEETING == id) {
            //xueqianag add for instance meeting
            final int nRet = (Integer) args[0];
            if (nRet == 0) {
                String meetingID = (String) args[1];
                startMeeting(meetingID);
            } else if (nRet == 4008) {
                //inputMeetingPassward(R.string.checkmeeting_error_4008);
            } else if (nRet == 4110) {
                //inputMeetingPassward(R.string.checkmeeting_error_4110);
            } else if (nRet == 4007) {
                errorTipDialog(R.string.checkmeeting_error_4007);
            } else if (nRet == 3001) {
                errorTipDialog(R.string.checkmeeting_error_3001);
            } else if (nRet == 3002) {
                errorTipDialog(R.string.checkmeeting_error_3002);
            } else if (nRet == 3003) {
                errorTipDialog(R.string.checkmeeting_error_3003);
            } else if (nRet == 4109) {
                errorTipDialog(R.string.checkmeeting_error_4109);
            } else if (nRet == 4103) {
                errorTipDialog(R.string.checkmeeting_error_4103);
            } else {
                errorTipDialog(R.string.WaitingForNetwork);
            }
        } else if (id == MessagesController.EnterMeeting_Complete || id == MessagesController.meeting_call_response) {
            Utitlties.HideProgressDialog(this.getActivity());
        }
    }

    public void errorTipDialog(int errorTipID) {
        AlertDialog.Builder build = new AlertDialog.Builder(this.getActivity());
        build.setTitle(getString(R.string.link_tip));
        build.setMessage(getString(errorTipID));
        build.setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                arg0.dismiss();
                //finish();
            }

        });
        build.show();
    }

    private void onPlayNextAudio(final int mid) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // 锟皆讹拷锟斤拷锟斤拷锟斤拷一锟斤拷锟斤拷频锟侥硷拷
                boolean bFound = false;
                for (int i = messages.size() - 1; i >= 0; i--) {
                    MessageObject obj = messages.get(i);
                    if (obj.messageOwner.out)// 只锟杰诧拷锟脚憋拷锟剿碉拷
                        continue;
                    if (obj.messageOwner.id == mid)// 锟斤拷锟斤拷锟揭碉拷原锟斤拷锟斤拷锟脚碉拷位锟矫ｏ拷锟斤拷锟脚诧拷锟斤拷锟斤拷一锟斤拷锟斤拷频锟斤拷锟斤拷
                    {
                        obj.messageOwner.isPlaying = false;
                        bFound = true;
                        continue;
                    }
                    if (bFound) {
                        if (obj.messageOwner.media != null
                                && obj.messageOwner.media instanceof TLRPC.TL_messageMediaAudio) {
                            if (!obj.messageOwner.isRead)// 没锟叫讹拷锟斤拷锟斤拷
                            {
                                obj.messageOwner.isPlaying = true;
                                MediaController.getInstance().playAudio(obj);
                                break;
                            }
                        }
                    } else {
                        disableProximitySensor(false);
                        mProximitySensorListener.clearPendingRequests();
                        mProximitySensorListener.postDelayed(
                                mProximitySensorListener.mUnblankRunnable,
                                PROXIMITY_UNBLANK_DELAY_MILLIS);
                    }
                }
            }
        }, 250);
    }

    private void updateContactStatus() {
        if (topPanel == null) {//锟斤拷锟斤拷锟斤拷帧锟斤拷锟斤拷
            return;
        }
        if (currentUser == null) {
            topPanel.setVisibility(View.GONE);
        } else {
            if (currentEncryptedChat != null
                    && !(currentEncryptedChat instanceof TLRPC.TL_encryptedChat)
                    || currentUser.id / 1000 == 333
                    || (currentUser.phone != null
                    && currentUser.phone.length() != 0
                    && ContactsController.getInstance().contactsDict
                    .get(currentUser.id) != null && (ContactsController
                    .getInstance().contactsDict.size() != 0 || !ContactsController
                    .getInstance().loadingContacts))) {
                topPanel.setVisibility(View.GONE);
            } else {
                topPanel.setVisibility(View.VISIBLE);
                topPanelText.setShadowLayer(1, 0, Utilities.dp(1), 0xff8797a3);
                if (isCustomTheme) {
                    topPlaneClose
                            .setImageResource(R.drawable.ic_msg_btn_cross_custom);
                    topPanel.setBackgroundResource(R.drawable.top_pane_custom);
                } else {
                    topPlaneClose
                            .setImageResource(R.drawable.ic_msg_btn_cross_custom);
                    topPanel.setBackgroundResource(R.drawable.top_pane);
                }
                // 锟斤拷锟截癸拷锟斤拷锟揭碉拷锟斤拷片锟酵硷拷为锟斤拷系锟剿癸拷锟斤拷锟斤拷
                // if (currentUser.phone != null && currentUser.phone.length()
                // != 0) {
                // if
                // (MessagesController.getInstance().hidenAddToContacts.get(currentUser.id)
                // != null) {
                // topPanel.setVisibility(View.INVISIBLE);
                // } else {
                // topPanelText.setText(LocaleController.getString("AddToContacts",
                // R.string.AddToContacts));
                // topPlaneClose.setVisibility(View.VISIBLE);
                // topPlaneClose.setOnClickListener(new View.OnClickListener() {
                // @Override
                // public void onClick(View v) {
                // MessagesController.getInstance().hidenAddToContacts.put(currentUser.id,
                // currentUser);
                // topPanel.setVisibility(View.GONE);
                // }
                // });
                // topPanel.setOnClickListener(new View.OnClickListener() {
                // @Override
                // public void onClick(View v) {
                // if (parentActivity == null) {
                // return;
                // }
                // ContactAddActivity fragment = new ContactAddActivity();
                // Bundle args = new Bundle();
                // args.putInt("user_id", currentUser.id);
                // fragment.setArguments(args);
                // ((LaunchActivity)parentActivity).presentFragment(fragment,
                // "add_contact_" + currentUser.id, false);
                // }
                // });
                // }
                // } else {
                // if
                // (MessagesController.getInstance().hidenAddToContacts.get(currentUser.id)
                // != null) {
                // topPanel.setVisibility(View.INVISIBLE);
                // } else {
                // topPanelText.setText(LocaleController.getString("ShareMyContactInfo",
                // R.string.ShareMyContactInfo));
                // topPlaneClose.setVisibility(View.GONE);
                // topPanel.setOnClickListener(new View.OnClickListener() {
                // @Override
                // public void onClick(View v) {
                // MessagesController.getInstance().hidenAddToContacts.put(currentUser.id,
                // currentUser);
                // topPanel.setVisibility(View.GONE);
                // MessagesController.getInstance().sendMessage(UserConfig.currentUser,
                // dialog_id);
                // chatListView.post(new Runnable() {
                // @Override
                // public void run() {
                // chatListView.setSelectionFromTop(messages.size() - 1, -100000
                // - chatListView.getPaddingTop());
                // }
                // });
                // }
                // });
                // }
                // }
                topPanel.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void createEmojiPopup() {
        emojiView = new EmojiView(parentActivity);
        emojiView.setListener(new EmojiView.Listener() {
            public void onBackspace() {
                messsageEditText.dispatchKeyEvent(new KeyEvent(0, 67));
            }

            public void onEmojiSelected(String paramAnonymousString) {
                int i = messsageEditText.getSelectionEnd();
                CharSequence localCharSequence = Emoji
                        .replaceEmoji(paramAnonymousString);
                messsageEditText.setText(messsageEditText.getText().insert(i,
                        localCharSequence));
                int j = i + localCharSequence.length();
                messsageEditText.setSelection(j, j);
            }
        });
        emojiPopup = new PopupWindow(emojiView);
    }

    private void showEmojiPopup(boolean show) {
        if (parentActivity == null) {
            return;
        }
        InputMethodManager localInputMethodManager = (InputMethodManager) parentActivity
                .getSystemService("input_method");
        if (show) {
            if (emojiPopup == null) {
                createEmojiPopup();
            }
            int currentHeight;
            WindowManager manager = (WindowManager) ApplicationLoader.applicationContext
                    .getSystemService(Activity.WINDOW_SERVICE);
            int rotation = manager.getDefaultDisplay().getRotation();
            if (keyboardHeight <= 0) {
                keyboardHeight = parentActivity
                        .getSharedPreferences("emoji", 0).getInt("kbd_height",
                                Emoji.scale(200.0f));
            }
            if (keyboardHeightLand <= 0) {
                keyboardHeightLand = parentActivity.getSharedPreferences(
                        "emoji", 0).getInt("kbd_height_land3",
                        Emoji.scale(200.0f));
            }
            if (rotation == Surface.ROTATION_270
                    || rotation == Surface.ROTATION_90) {
                currentHeight = keyboardHeightLand;
            } else {
                currentHeight = keyboardHeight;
            }
            emojiPopup.setHeight(View.MeasureSpec.makeMeasureSpec(
                    currentHeight, View.MeasureSpec.EXACTLY));
            emojiPopup.setWidth(View.MeasureSpec.makeMeasureSpec(
                    contentView.getWidth(), View.MeasureSpec.EXACTLY));

            emojiPopup.showAtLocation(
                    parentActivity.getWindow().getDecorView(), 83, 0, 0);
            if (!keyboardVisible) {
                contentView.setPadding(0, 0, 0, currentHeight);
                emojiButton.setImageResource(R.drawable.ic_msg_panel_hide);
                return;
            }
            emojiButton.setImageResource(R.drawable.ic_msg_panel_kb);
            return;
        }
        if (emojiButton != null) {
            emojiButton.setImageResource(R.drawable.ic_msg_panel_smiles);
        }
        if (emojiPopup != null) {
            emojiPopup.dismiss();
        }
        if (contentView != null) {
            contentView.post(new Runnable() {
                public void run() {
                    if (contentView != null) {
                        contentView.setPadding(0, 0, 0, 0);
                    }
                }
            });
        }
    }

    public void hideEmojiPopup() {
        if (emojiPopup != null && emojiPopup.isShowing()) {
            showEmojiPopup(false);
        }
    }

    @Override
    public void applySelfActionBar() {
        if (parentActivity == null) {
            return;
        }
        ActionBar actionBar = super.applySelfActionBar(true);
        updateSubtitle();
        ((LaunchActivity) parentActivity).fixBackButton();
    }

    @Override
    public void onDestroyView() {
        FileLog.e("emm", "chatactivity ondestoryview");
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        FileLog.e("emm", "chatactivity ondestory");
        super.onDestroy();
        // 锟剿筹拷时锟酵凤拷锟斤拷锟斤拷
        if (mIat != null) {
            mIat.cancel();
            mIat.destroy();
        }
    }

    @Override
    public void onStop() {
        //FileLog.e("emm", "chatactivity onstop");
        super.onStop();
    }


    @Override
    public void onResume() {
        super.onResume();
        //SpeechUtility.getUtility().checkServiceInstalled();
        //FileLog.e("emm", "chatactivity onresume");
        if (isFinish) {
            return;
        }
        if (!firstStart && chatAdapter != null) {
            chatAdapter.notifyDataSetChanged();
        }
        MessagesController.getInstance().openned_dialog_id = dialog_id;
        if (scrollToTopOnResume) {
            if (scrollToTopUnReadOnResume && unreadMessageObject != null) {
                if (chatListView != null) {
                    chatListView.setSelectionFromTop(
                            messages.size()
                                    - messages.indexOf(unreadMessageObject),
                            -chatListView.getPaddingTop() - Utilities.dp(7));
                }
            } else {
                if (chatListView != null) {
                    chatListView.setSelection(messages.size() + 1);
                }
            }
            scrollToTopUnReadOnResume = false;
            scrollToTopOnResume = false;
        }
        firstStart = false;
        swipeOpening = false;
        if (emojiView != null) {
            emojiView.loadRecents();
        }
        paused = false;
        if (readWhenResume && !messages.isEmpty()) {
            readWhenResume = false;
            MessagesController.getInstance().markDialogAsRead(dialog_id,
                    messages.get(0).messageOwner.id, readWithMid, 0,
                    readWithDate, true);
        }
        if (getActivity() == null) {
            return;
        }
        ((LaunchActivity) parentActivity).showActionBar();
        ((LaunchActivity) parentActivity).updateActionBar();
        fixLayout();
        SharedPreferences preferences = ApplicationLoader.applicationContext
                .getSharedPreferences("mainconfig_" + UserConfig.clientUserId,
                        Activity.MODE_PRIVATE);
        String lastMessageText = preferences.getString("dialog_" + dialog_id,
                null);
        if (lastMessageText != null) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.remove("dialog_" + dialog_id);
            editor.commit();
            ignoreTextChange = true;
            messsageEditText.setText(lastMessageText);
            messsageEditText.setSelection(messsageEditText.getText().length());
            ignoreTextChange = false;
        }
        if (messsageEditText != null) {
            messsageEditText.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (messsageEditText != null) {
                        messsageEditText.requestFocus();
                    }
                }
            }, 400);
        }


        if (!WeiyiMeeting.isInMeeting()) {
            MediaController.getInstance().SetPlayoutSpeaker(!MessagesController.getInstance().earphone);
        }

        mProximitySensorListener.clearPendingRequests();


        // mProximitySensorListener.mUnblankRunnable.run();

        //		FileLog.e("emm", "chatactivity onResume");
        // if (currentEncryptedChat != null && parentActivity != null) {
        // parentActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        // }
        //锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟节伙拷锟斤拷锟斤拷锟斤拷示锟斤拷示锟斤拷目
        callViewSet();
    }

    private void setTypingAnimation(boolean start) {
        TextView subtitle = (TextView) parentActivity
                .findViewById(R.id.action_bar_subtitle);
        if (subtitle == null) {
            final int subtitleId = parentActivity.getResources().getIdentifier(
                    "action_bar_subtitle", "id", "android");
            subtitle = (TextView) parentActivity.findViewById(subtitleId);
        }
        if (subtitle != null) {
            if (start) {
                try {
                    if (currentChat != null) {
                        subtitle.setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.typing_dots_chat, 0, 0, 0);
                    } else {
                        subtitle.setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.typing_dots, 0, 0, 0);
                    }
                    subtitle.setCompoundDrawablePadding(Utilities.dp(4));
                    AnimationDrawable mAnim = (AnimationDrawable) subtitle
                            .getCompoundDrawables()[0];
                    mAnim.setAlpha(200);
                    mAnim.start();
                } catch (Exception e) {
                    FileLog.e("emm", e);
                }
            } else {
                subtitle.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            }
        }
    }

    @Override
    public void enableProximitySensor() {
        mProximitySensorManager.enable();
    }

    @Override
    public void disableProximitySensor(boolean waitForFarState) {
        mProximitySensorManager.disable(waitForFarState);
    }

    @Override
    public void onPause() {
        super.onPause();
        FileLog.e("emm", "onpause");
        if (mActionMode != null) {
            mActionMode.finish();
            mActionMode = null;
        }
        hideEmojiPopup();
        paused = true;
        MessagesController.getInstance().openned_dialog_id = 0;

        if (messsageEditText != null && messsageEditText.length() != 0) {
            SharedPreferences preferences = ApplicationLoader.applicationContext
                    .getSharedPreferences("mainconfig_"
                            + UserConfig.clientUserId, Activity.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("dialog_" + dialog_id, messsageEditText.getText()
                    .toString());
            editor.commit();
        }

        disableProximitySensor(false);
        mProximitySensorListener.clearPendingRequests();
        mProximitySensorListener.mUnblankRunnable.run();
        MediaController.getInstance().SetPlayoutSpeaker(false);

        // if (currentEncryptedChat != null && parentActivity != null) {
        // parentActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        // }
    }

    private void fixLayout() {
        if (chatListView != null) {
            ViewTreeObserver obs = chatListView.getViewTreeObserver();
            obs.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    if (parentActivity == null) {
                        chatListView.getViewTreeObserver()
                                .removeOnPreDrawListener(this);
                        return false;
                    }
                    WindowManager manager = (WindowManager) ApplicationLoader.applicationContext
                            .getSystemService(Activity.WINDOW_SERVICE);
                    Display display = manager.getDefaultDisplay();
                    int rotation = Surface.ROTATION_0;
                    if (display != null) {
                        rotation = display.getRotation();
                    }
                    int height;
                    int currentActionBarHeight = parentActivity
                            .getSupportActionBar().getHeight();

                    if (currentActionBarHeight != Utilities.dp(48)
                            && currentActionBarHeight != Utilities.dp(40)) {
                        height = currentActionBarHeight;
                    } else {
                        height = Utilities.dp(48);
                        if (rotation == Surface.ROTATION_270
                                || rotation == Surface.ROTATION_90) {
                            height = Utilities.dp(40);
                        }
                    }

                    if (avatarImageView != null) {
                        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) avatarImageView
                                .getLayoutParams();
                        params.width = height;
                        params.height = height;
                        avatarImageView.setLayoutParams(params);
                    }

                    chatListView.getViewTreeObserver().removeOnPreDrawListener(
                            this);

                    if (currentEncryptedChat != null) {
                        TextView title = (TextView) parentActivity
                                .findViewById(R.id.action_bar_title);
                        if (title == null) {
                            final int subtitleId = parentActivity
                                    .getResources()
                                    .getIdentifier("action_bar_title", "id",
                                            "android");
                            title = (TextView) parentActivity
                                    .findViewById(subtitleId);
                        }
                        if (title != null) {
                            title.setCompoundDrawablesWithIntrinsicBounds(
                                    R.drawable.ic_lock_white, 0, 0, 0);
                            title.setCompoundDrawablePadding(Utilities.dp(4));
                        }
                    }

                    return false;
                }
            });
        }
    }

    @Override
    public void onConfigurationChanged(
            android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        fixLayout();

        if (parentActivity != null) {
            Display display = parentActivity.getWindowManager()
                    .getDefaultDisplay();
            if (android.os.Build.VERSION.SDK_INT < 13) {
                displaySize.set(display.getWidth(), display.getHeight());
            } else {
                display.getSize(displaySize);
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        FileLog.e("emm", "chat onCreateOptionsMenu**************************");
        //super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        if (currentEncryptedChat != null) {
            inflater.inflate(R.menu.chat_enc_menu, menu);
        } else {
            inflater.inflate(R.menu.chat_menu, menu);
        }
        SupportMenuItem timeItem = (SupportMenuItem) menu
                .findItem(R.id.chat_enc_timer);

        SupportMenuItem callItem = (SupportMenuItem) menu
                .findItem(R.id.attach_audio_call);
        if (callItem != null) {
            callItem.setVisible(toCallLimit);
        }
        SupportMenuItem groupChat = (SupportMenuItem) menu.findItem(R.id.attach_group_chat);
        if (currentUser == null) {
            groupChat.setVisible(false);
        }

        if (currentEncryptedChat != null
                && !(currentEncryptedChat instanceof TLRPC.TL_encryptedChat)
                || currentChat != null
                && (currentChat instanceof TLRPC.TL_chatForbidden || currentChat.left)) {
            MenuItem item = menu.findItem(R.id.chat_menu_attach);
            MenuItem item2 = menu.findItem(R.id.attach_audio_call);
            if (item != null && item2 != null) {
                item.setVisible(false);
                item2.setVisible(false);
            }

            if (timeItem != null) {
                timeItem.setVisible(false);
            }
        }

        if (timeItem != null && timeItem.getActionView() != null) {
            timerButton = (TimerButton) timeItem.getActionView().findViewById(
                    R.id.chat_timer);
            timerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(
                            parentActivity);
                    builder.setTitle(LocaleController.getString(
                            "MessageLifetime", R.string.MessageLifetime));
                    builder.setItems(
                            new CharSequence[]{
                                    LocaleController
                                            .getString(
                                            "ShortMessageLifetimeForever",
                                            R.string.ShortMessageLifetimeForever),
                                    LocaleController.getString(
                                            "ShortMessageLifetime2s",
                                            R.string.ShortMessageLifetime2s),
                                    LocaleController.getString(
                                            "ShortMessageLifetime5s",
                                            R.string.ShortMessageLifetime5s),
                                    LocaleController.getString(
                                            "ShortMessageLifetime1m",
                                            R.string.ShortMessageLifetime1m),
                                    LocaleController.getString(
                                            "ShortMessageLifetime1h",
                                            R.string.ShortMessageLifetime1h),
                                    LocaleController.getString(
                                            "ShortMessageLifetime1d",
                                            R.string.ShortMessageLifetime1d),
                                    LocaleController.getString(
                                            "ShortMessageLifetime1w",
                                            R.string.ShortMessageLifetime1w)

                            }, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    int oldValue = currentEncryptedChat.ttl;
                                    if (which == 0) {
                                        currentEncryptedChat.ttl = 0;
                                    } else if (which == 1) {
                                        currentEncryptedChat.ttl = 2;
                                    } else if (which == 2) {
                                        currentEncryptedChat.ttl = 5;
                                    } else if (which == 3) {
                                        currentEncryptedChat.ttl = 60;
                                    } else if (which == 4) {
                                        currentEncryptedChat.ttl = 60 * 60;
                                    } else if (which == 5) {
                                        currentEncryptedChat.ttl = 60 * 60 * 24;
                                    } else if (which == 6) {
                                        currentEncryptedChat.ttl = 60 * 60 * 24 * 7;
                                    }
                                    if (oldValue != currentEncryptedChat.ttl) {
                                        MessagesController.getInstance()
                                                .sendTTLMessage(
                                                        currentEncryptedChat);
                                        MessagesStorage.getInstance()
                                                .updateEncryptedChat(
                                                        currentEncryptedChat);
                                    }
                                }
                            });
                    builder.setNegativeButton(LocaleController.getString(
                            "Cancel", R.string.Cancel), null);
                    builder.show().setCanceledOnTouchOutside(true);
                }
            });
            timerButton.setTime(currentEncryptedChat.ttl);
        }

        SupportMenuItem avatarItem = (SupportMenuItem) menu
                .findItem(R.id.chat_menu_avatar);
        View avatarLayout = avatarItem.getActionView();
        avatarImageView = (BackupImageView) avatarLayout
                .findViewById(R.id.chat_avatar_image);

        avatarImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (parentActivity == null) {
                    return;
                }
                if (currentUser != null) {
                    UserProfileActivity fragment = new UserProfileActivity();
                    Bundle args = new Bundle();
                    args.putInt("user_id", currentUser.id);
                    if (currentEncryptedChat != null) {
                        args.putLong("dialog_id", dialog_id);
                    }
                    args.putInt(Config.CompanyID, mCompanyID);
                    args.putInt(Config.DeptID, mDeptID);
                    args.putBoolean("from_chatface", true);
                    fragment.setArguments(args);
                    ((LaunchActivity) parentActivity).presentFragment(fragment,
                            "user_" + currentUser.id, swipeOpening);
                } else if (currentChat != null) {
                    if (chatinfo != null) {
                        if (chatinfo instanceof TLRPC.TL_chatParticipantsForbidden) {
                            return;
                        }
                        NotificationCenter.getInstance().addToMemCache(5, chatinfo);
                    }
                    if (currentChat.participants_count == 0 || currentChat.left
                            || currentChat instanceof TLRPC.TL_chatForbidden) {
                        return;
                    }
                    ChatProfileActivity fragment = new ChatProfileActivity();
                    Bundle args = new Bundle();
                    args.putInt("chat_id", currentChat.id);
                    //args.putInt(Config.CompanyID, currentChat.companyid); // 锟斤拷锟捷碉拷前锟斤拷锟絚ompanyID
                    fragment.setArguments(args);
                    ((LaunchActivity) parentActivity).presentFragment(fragment,
                            "chat_" + currentChat.id, swipeOpening);
                }
            }
        });

        TLRPC.FileLocation photo = null;
        int placeHolderId = 0;
        if (currentUser != null) {
            if (currentUser.photo != null) {
                photo = currentUser.photo.photo_small;    //qxm change
            }
            placeHolderId = R.drawable.user_head;//Utilities.getUserAvatarForId(currentUser.id);
        } else if (currentChat != null) {
            if (currentChat.photo != null) {
                photo = currentChat.photo.photo_small;
            }
            placeHolderId = R.drawable.group_head;//Utilities.getGroupAvatarForId(currentChat.id);
        }
        avatarImageView.setImage(photo, "50_50", placeHolderId);


    }

    private View getRowParentView(View v) {
        if (v instanceof ChatBaseCell) {
            return v;
        } else {
            while (!(v.getTag() instanceof ChatListRowHolderEx)) {
                ViewParent parent = v.getParent();
                if (!(parent instanceof View)) {
                    return null;
                }
                v = (View) v.getParent();
                if (v == null) {
                    return null;
                }
            }
            return v;
        }
    }

    public void createMenu(View v, boolean single) {
        if (mActionMode != null || parentActivity == null
                || getActivity() == null || isFinish || swipeOpening) {
            return;
        }

        selectedMessagesCanCopyIds.clear();
        selectedMessagesCanNotForwardIds.clear();
        selectedObject = null;
        forwaringMessage = null;
        selectedMessagesIds.clear();
        View parentView = getRowParentView(v);
        if (parentView == null) {
            return;
        }
        MessageObject message = null;
        if (v instanceof ChatBaseCell) {
            message = ((ChatBaseCell) v).getMessageObject();
        } else {
            ChatListRowHolderEx holder = (ChatListRowHolderEx) parentView
                    .getTag();
            message = holder.message;
        }

        final int type = getMessageType(message);
        if (single || type < 2) {
            if (type >= 0) {
                selectedObject = message;
                AlertDialog.Builder builder = new AlertDialog.Builder(
                        getActivity());

                CharSequence[] items = null;

                if (currentEncryptedChat == null) {
                    if (type == 0) {
                        items = new CharSequence[]{
                                LocaleController.getString("Retry",
                                        R.string.Retry),
                                LocaleController.getString("Delete",
                                        R.string.Delete)};
                    } else if (type == 1) {
                        items = new CharSequence[]{LocaleController
                                .getString("Delete", R.string.Delete)};
                    } else if (type == 2) {
                        items = new CharSequence[]{
                                StringUtil.getStringFromRes(MessagesController.getInstance().earphone ? R.string.chatfooter_SpeakerOff : R.string.chatfooter_SpeakerON),
                                LocaleController.getString("Forward",
                                        R.string.Forward),
                                LocaleController.getString("Delete",
                                        R.string.Delete)};
                    } else if (type == 3) {
                        items = new CharSequence[]{
                                LocaleController.getString("Forward",
                                        R.string.Forward),
                                LocaleController.getString("Copy",
                                        R.string.Copy),
                                LocaleController.getString("Delete",
                                        R.string.Delete)};
                    } else if (type == 4) {
                        items = new CharSequence[]{
                                LocaleController
                                        .getString(
                                        selectedObject.messageOwner.media instanceof TLRPC.TL_messageMediaDocument ? "SaveToDownloads"
                                                : "SaveToGallery",
                                        selectedObject.messageOwner.media instanceof TLRPC.TL_messageMediaDocument ? R.string.SaveToDownloads
                                                : R.string.SaveToGallery),
                                LocaleController.getString("Forward",
                                        R.string.Forward),
                                LocaleController.getString("Delete",
                                        R.string.Delete)};
                    } else if (type == 5) {  //锟斤拷锟斤拷
                        items = new CharSequence[]{LocaleController.getString("Delete",
                                R.string.Delete)};
                    }
                } else {
                    if (type == 0) {
                        items = new CharSequence[]{
                                LocaleController.getString("Retry",
                                        R.string.Retry),
                                LocaleController.getString("Delete",
                                        R.string.Delete)};
                    } else if (type == 1) {
                        items = new CharSequence[]{LocaleController
                                .getString("Delete", R.string.Delete)};
                    } else if (type == 2) {
                        items = new CharSequence[]{LocaleController
                                .getString("Delete", R.string.Delete)};
                    } else if (type == 3) {
                        items = new CharSequence[]{
                                LocaleController.getString("Copy",
                                        R.string.Copy),
                                LocaleController.getString("Delete",
                                        R.string.Delete)};
                    } else if (type == 4) {
                        items = new CharSequence[]{
                                LocaleController
                                        .getString(
                                        selectedObject.messageOwner.media instanceof TLRPC.TL_messageMediaDocument ? "SaveToDownloads"
                                                : "SaveToGallery",
                                        selectedObject.messageOwner.media instanceof TLRPC.TL_messageMediaDocument ? R.string.SaveToDownloads
                                                : R.string.SaveToGallery),
                                LocaleController.getString("Delete",
                                        R.string.Delete)};
                    } else if (type == 5) {
                        items = new CharSequence[]{LocaleController.getString("Delete",
                                R.string.Delete)};
                    }
                }

                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (type == 0) {
                            if (i == 0) {
                                processSelectedOption(0);
                            } else if (i == 1) {
                                processSelectedOption(1);
                            }
                        } else if (type == 1) {
                            processSelectedOption(1);
                        } else if (type == 2) {
                            if (currentEncryptedChat == null) {
                                if (i == 0) {
                                    processSelectedOption(4);
                                } else if (i == 1) {
                                    processSelectedOption(2);
                                } else if (i == 2) {
                                    processSelectedOption(1);
                                }
                            } else {
                                processSelectedOption(1);
                            }
                        } else if (type == 3) {
                            if (currentEncryptedChat == null) {
                                if (i == 0) {
                                    processSelectedOption(2);
                                } else if (i == 1) {
                                    processSelectedOption(3);
                                } else if (i == 2) {
                                    processSelectedOption(1);
                                }
                            } else {
                                if (i == 0) {
                                    processSelectedOption(3);
                                } else if (i == 1) {
                                    processSelectedOption(1);
                                }
                            }
                        } else if (type == 4) {
                            if (currentEncryptedChat == null) {
                                if (i == 0) {
                                    String fileName = selectedObject
                                            .getFileName();
                                    if (selectedObject.type == 6
                                            || selectedObject.type == 7) {
                                        MediaController.saveFile(fileName,
                                                parentActivity, 1, null);
                                    } else if (selectedObject.type == 2
                                            || selectedObject.type == 3) {
                                        MediaController.saveFile(fileName,
                                                parentActivity, 0, null);
                                    } else if (selectedObject.type == 16
                                            || selectedObject.type == 17) {
                                        MediaController
                                                .saveFile(
                                                        fileName,
                                                        parentActivity,
                                                        2,
                                                        selectedObject.messageOwner.media.document.file_name);
                                    }
                                } else if (i == 1) {
                                    processSelectedOption(2);
                                } else if (i == 2) {
                                    processSelectedOption(1);
                                }
                            } else {
                                if (i == 0) {

                                } else if (i == 1) {
                                    processSelectedOption(1);
                                }
                            }
                        } else if (type == 5) {
                            processSelectedOption(1);
                        }
                    }
                });

                builder.setTitle(R.string.Message);
                visibleDialog = builder.show();
                visibleDialog.setCanceledOnTouchOutside(true);

                visibleDialog
                        .setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                visibleDialog = null;
                            }
                        });
            }
            return;
        }
        addToSelectedMessages(message);
        mActionMode = parentActivity
                .startSupportActionMode(mActionModeCallback);
        updateActionModeTitle();
        updateVisibleRows();
    }

    private void processSelectedOption(int option) {
        if (option == 0) {
            if (selectedObject != null && selectedObject.messageOwner.id < 0) {
                if (selectedObject.type == 0 || selectedObject.type == 1) {
                    if (selectedObject.messageOwner instanceof TLRPC.TL_messageForwarded) {
                        MessagesController.getInstance().sendMessage(
                                selectedObject, dialog_id);
                    } else {
                        MessagesController.getInstance().sendMessage(
                                selectedObject.messageOwner.message, dialog_id);
                    }
                } else if (selectedObject.type == 8 || selectedObject.type == 9) {
                    MessagesController.getInstance().sendMessage(
                            selectedObject, dialog_id);
                } else if (selectedObject.type == 4 || selectedObject.type == 5) {
                    MessagesController.getInstance().sendMessage(
                            selectedObject.messageOwner.media.geo.lat,
                            selectedObject.messageOwner.media.geo._long,
                            dialog_id);
                } else if (selectedObject.type == 2 || selectedObject.type == 3) {
                    if (selectedObject.messageOwner instanceof TLRPC.TL_messageForwarded) {
                        MessagesController.getInstance().sendMessage(
                                selectedObject, dialog_id);
                    } else {
                        TLRPC.TL_photo photo = (TLRPC.TL_photo) selectedObject.messageOwner.media.photo;
                        MessagesController.getInstance().sendMessage(photo,
                                dialog_id);
                    }
                } else if (selectedObject.type == 6 || selectedObject.type == 7) {
                    if (selectedObject.messageOwner instanceof TLRPC.TL_messageForwarded) {
                        MessagesController.getInstance().sendMessage(
                                selectedObject, dialog_id);
                    } else {
                        TLRPC.TL_video video = (TLRPC.TL_video) selectedObject.messageOwner.media.video;
                        video.path = selectedObject.messageOwner.attachPath;
                        MessagesController.getInstance().sendMessage(video,
                                dialog_id);
                    }
                } else if (selectedObject.type == 12
                        || selectedObject.type == 13) {
                    TLRPC.User user = MessagesController.getInstance().users
                            .get(selectedObject.messageOwner.media.user_id);
                    MessagesController.getInstance().sendMessage(user,
                            dialog_id);
                } else if (selectedObject.type == 16
                        || selectedObject.type == 17) {
                    TLRPC.TL_document document = (TLRPC.TL_document) selectedObject.messageOwner.media.document;
                    document.path = selectedObject.messageOwner.attachPath;
                    MessagesController.getInstance().sendMessage(document,
                            dialog_id);
                } else if (selectedObject.type == 18
                        || selectedObject.type == 19) {
                    TLRPC.TL_audio audio = (TLRPC.TL_audio) selectedObject.messageOwner.media.audio;
                    audio.path = selectedObject.messageOwner.attachPath;
                    MessagesController.getInstance().sendMessage(audio,
                            dialog_id);
                }
                ArrayList<Integer> arr = new ArrayList<Integer>();
                arr.add(selectedObject.messageOwner.id);
                ArrayList<Long> random_ids = null;
                if (currentEncryptedChat != null
                        && selectedObject.messageOwner.random_id != 0) {
                    random_ids = new ArrayList<Long>();
                    random_ids.add(selectedObject.messageOwner.random_id);
                }
                // xueqiang change
                if (selectedObject.type == 18 || selectedObject.type == 19)
                    MessagesController.getInstance().deleteMessages(arr,
                            random_ids, currentEncryptedChat, false);
                else
                    MessagesController.getInstance().deleteMessages(arr,
                            random_ids, currentEncryptedChat, true);
                chatListView.setSelection(messages.size() + 1);
            }
        } else if (option == 1) {
            if (selectedObject != null) {
                ArrayList<Integer> ids = new ArrayList<Integer>();
                ids.add(selectedObject.messageOwner.id);
                removeUnreadPlane(true);
                ArrayList<Long> random_ids = null;
                if (currentEncryptedChat != null
                        && selectedObject.messageOwner.random_id != 0) {
                    random_ids = new ArrayList<Long>();
                    random_ids.add(selectedObject.messageOwner.random_id);
                }
                // xueqiang change
                MessagesController.getInstance().deleteMessages(ids,
                        random_ids, currentEncryptedChat, true);
                selectedObject = null;
            }
        } else if (option == 2) {
            if (selectedObject != null) {
                if (parentActivity == null) {
                    return;
                }
                forwaringMessage = selectedObject;
                selectedObject = null;

                MessagesActivity fragment = new MessagesActivity();
                fragment.selectAlertString = R.string.ForwardMessagesTo;
                fragment.selectAlertStringDesc = "ForwardMessagesTo";
                fragment.animationType = 1;
                Bundle args = new Bundle();
                args.putBoolean("onlySelect", true);
                args.putBoolean("serverOnly", true);
                args.putString("typeFace", "retransmit");
                fragment.setArguments(args);
                fragment.delegate = this;
                ((LaunchActivity) parentActivity).presentFragment(fragment,
                        "select_chat", false);
            }
        } else if (option == 3) {
            if (selectedObject != null) {
                if (android.os.Build.VERSION.SDK_INT < 11) {
                    android.text.ClipboardManager clipboard = (android.text.ClipboardManager) parentActivity
                            .getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboard.setText(selectedObject.messageText);
                } else {
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) parentActivity
                            .getSystemService(Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData
                            .newPlainText("label", selectedObject.messageText);
                    clipboard.setPrimaryClip(clip);
                }
                selectedObject = null;
            }
        } else if (option == 4) {
            //锟斤拷筒 锟斤拷锟斤拷锟斤拷 锟叫伙拷,锟斤拷锟斤拷锟斤拷锟轿拷锟酵材Ｊ斤拷锟斤拷锟斤拷为锟斤拷筒模式锟斤拷锟斤拷锟斤拷锟斤拷锟轿拷锟斤拷锟斤拷锟侥Ｊ斤拷锟斤拷锟斤拷锟揭拷远锟斤拷锟斤拷锟�
            //锟斤拷锟斤拷锟斤拷锟斤拷锟竭撅拷为锟斤拷筒模式锟斤拷锟诫开锟斤拷锟竭撅拷为锟斤拷锟斤拷锟斤拷模式
            SharedPreferences preferences = ApplicationLoader.applicationContext
                    .getSharedPreferences(
                            "mainconfig_"
                                    + UserConfig.clientUserId,
                            Activity.MODE_PRIVATE);
            boolean earphone = preferences.getBoolean("earphone", false);

            if (MediaController.getInstance().SetPlayoutSpeaker(earphone) == 0) {
                earphone = !earphone;
                SharedPreferences.Editor editor = preferences
                        .edit();
                editor.putBoolean("earphone", earphone);
                editor.commit();
                MessagesController.getInstance().earphone = earphone;
                updateSubtitle();
            }

        }
    }

    @Override
    public void didSelectFile(DocumentSelectActivity activity, String path,
                              String name, String ext, long size) {
        activity.finishFragment();
        TLRPC.TL_document document = new TLRPC.TL_document();
        document.thumb = new TLRPC.TL_photoSizeEmpty();
        document.thumb.type = "s";
        document.id = 0;
        document.user_id = UserConfig.clientUserId;
        document.date = ConnectionsManager.getInstance().getCurrentTime();
        document.file_name = name;
        document.size = (int) size;
        document.dc_id = 0;
        document.path = path;
        if (ext.length() != 0) {
            MimeTypeMap myMime = MimeTypeMap.getSingleton();
            String mimeType = myMime
                    .getMimeTypeFromExtension(ext.toLowerCase());
            if (mimeType != null) {
                document.mime_type = mimeType;
            } else {
                document.mime_type = "application/octet-stream";
            }
        } else {
            document.mime_type = "application/octet-stream";
        }
        MessagesController.getInstance().sendMessage(document, dialog_id);
    }

    @Override
    public void didSelectDialog(MessagesActivity activity, long did) {
        if (dialog_id != 0
                && (forwaringMessage != null || !selectedMessagesIds.isEmpty())) {
            if (did != dialog_id) {
                int lower_part = (int) did;
                if (lower_part != 0) {
                    ActionBarActivity inflaterActivity = parentActivity;
                    if (inflaterActivity == null) {
                        inflaterActivity = (ActionBarActivity) getActivity();
                    }
                    activity.removeSelfFromStack();
                    ChatActivity fragment = new ChatActivity();
                    Bundle bundle = new Bundle();
                    if (lower_part > 0) {
                        bundle.putInt("user_id", lower_part);
                        fragment.setArguments(bundle);
                        fragment.scrollToTopOnResume = true;
                        ActionBarActivity act = (ActionBarActivity) getActivity();
                        if (inflaterActivity != null) {
                            ((LaunchActivity) inflaterActivity)
                                    .presentFragment(fragment,
                                            "chat" + Math.random(), false);
                        }
                    } else if (lower_part < 0) {
                        bundle.putInt("chat_id", -lower_part);
                        fragment.setArguments(bundle);
                        fragment.scrollToTopOnResume = true;
                        if (inflaterActivity != null) {
                            ((LaunchActivity) inflaterActivity)
                                    .presentFragment(fragment,
                                            "chat" + Math.random(), false);
                        }
                    }
                    removeSelfFromStack();
                    if (forwaringMessage != null) {
                        if (forwaringMessage.messageOwner.id > 0) {
                            MessagesController.getInstance().sendMessage(
                                    forwaringMessage, did);
                        }
                        forwaringMessage = null;
                    } else {
                        ArrayList<Integer> ids = new ArrayList<Integer>(
                                selectedMessagesIds.keySet());
                        Collections.sort(ids);
                        for (Integer id : ids) {
                            if (id > 0) {
                                MessagesController.getInstance().sendMessage(
                                        selectedMessagesIds.get(id), did);
                            }
                        }
                        selectedMessagesIds.clear();
                    }
                } else {
                    activity.finishFragment();
                }
            } else {
                activity.finishFragment();
                if (forwaringMessage != null) {
                    MessagesController.getInstance().sendMessage(
                            forwaringMessage, did);
                    forwaringMessage = null;
                } else {
                    ArrayList<Integer> ids = new ArrayList<Integer>(
                            selectedMessagesIds.keySet());
                    Collections.sort(ids, new Comparator<Integer>() {
                        @Override
                        public int compare(Integer lhs, Integer rhs) {
                            return lhs.compareTo(rhs);
                        }
                    });
                    for (Integer id : ids) {
                        MessagesController.getInstance().sendMessage(
                                selectedMessagesIds.get(id), did);
                    }
                    selectedMessagesIds.clear();
                }
                chatListView.setSelection(messages.size() + 1);
                scrollToTopOnResume = true;
            }
        }
    }

    @Override
    public boolean onBackPressed() {
        FileLog.d("emm", "chat onBackPressed");
        if (emojiPopup != null && emojiPopup.isShowing()) {
            hideEmojiPopup();
            return false;
        } else {
            return true;
        }
    }

    private void showTip(final String str) {
        Utilities.RunOnUIThread(new Runnable() {
            @Override
            public void run() {
                mToast.setText(str);
                mToast.show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finishFragment();

        } else if (itemId == R.id.attach_photo) {
            try {
                //锟斤拷锟斤拷然锟斤拷锟较达拷
                Intent takePictureIntent = new Intent(
                        MediaStore.ACTION_IMAGE_CAPTURE);
                File image = Utilities.generatePicturePath();
                if (image != null) {
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                            Uri.fromFile(image));
                    currentPicturePath = image.getAbsolutePath();
                }
                startActivityForResult(takePictureIntent, 0);
            } catch (Exception e) {
                FileLog.e("emm", e);
            }
        } else if (itemId == R.id.attach_gallery) {
            try {
                //选锟今本碉拷图片然锟斤拷锟较达拷,xueqiang
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, 1);
            } catch (Exception e) {
                FileLog.e("emm", e);
            }
        } else if (itemId == R.id.attach_audio_input) {
            if (mIat == null) {
                SpeechUtility.createUtility(this.parentActivity, "appid=54069248");
                mIat = SpeechRecognizer.createRecognizer(this.parentActivity, mInitListener);
                // 锟斤拷始锟斤拷锟斤拷写Dialog,锟斤拷锟街皇癸拷锟斤拷锟経I锟斤拷写锟斤拷锟斤拷,锟斤拷锟借创锟斤拷SpeechRecognizer
                iatDialog = new RecognizerDialog(this.parentActivity, mInitListener);
                //iatDialog.setTitle("锟狡达拷讯锟斤拷");
            }
            setParam();
            iatDialog.setListener(recognizerDialogListener);
            iatDialog.show();
        } else if (itemId == R.id.attach_video) {
            try {
                Intent pickIntent = new Intent();
                pickIntent.setType("video/*");
                pickIntent.setAction(Intent.ACTION_GET_CONTENT);
                pickIntent.putExtra(MediaStore.EXTRA_SIZE_LIMIT,
                        (long) (1024 * 1024 * 1000));
                Intent takeVideoIntent = new Intent(
                        MediaStore.ACTION_VIDEO_CAPTURE);
                File video = Utilities.generateVideoPath();
                if (video != null) {
                    if (android.os.Build.VERSION.SDK_INT > 16) {
                        takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                                Uri.fromFile(video));
                    }
                    takeVideoIntent.putExtra(MediaStore.EXTRA_SIZE_LIMIT,
                            (long) (1024 * 1024 * 1000));
                    currentPicturePath = video.getAbsolutePath();
                }
                Intent chooserIntent = Intent.createChooser(pickIntent, "");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS,
                        new Intent[]{takeVideoIntent});

                startActivityForResult(chooserIntent, 2);
            } catch (Exception e) {
                FileLog.e("emm", e);
            }
        } else if (itemId == R.id.attach_location) {
            AmapLocationActivity fragment = new AmapLocationActivity();
            ((LaunchActivity) parentActivity).presentFragment(fragment,
                    "location", false);
        } else if (itemId == R.id.attach_document) {
            DocumentSelectActivity fragment = new DocumentSelectActivity();
            fragment.delegate = this;
            ((LaunchActivity) parentActivity).presentFragment(fragment,
                    "document", false);
        } else if (itemId == R.id.attach_audio_call) {//锟斤拷锟斤拷checkmeeting锟斤拷取锟斤拷锟斤拷锟斤拷耄拷锟斤拷锟绞癸拷么锟絊ESSION锟斤拷CLIENT
            String mid = "";
            if (currentChat != null)
                mid = "g" + currentChat.id;
            else if (this.currentUser != null) {
                if (currentUser.id < UserConfig.clientUserId)
                    mid = "u" + currentUser.id + UserConfig.clientUserId;
                else
                    mid = "u" + UserConfig.clientUserId + currentUser.id;
            }
            //startMeeting(mid);
//			ApplicationLoader.getInstance().joinMeeting(getActivity(), mid, "");
//			MeetingMgr.getInstance().checkMeeting(mid, "", true);
            startMeeting(mid);

        } else if (itemId == R.id.attach_group_chat) {
            if (currentUser != null) {
                ((LaunchActivity) parentActivity).createNewgroup(currentUser.id);

                finishFragment();
            }

        } else if (itemId == R.id.attach_remind) {//			if(currentUser != null){
            RemindActivity fragment = new RemindActivity();
            Bundle bundle = new Bundle();
            bundle.putLong("dialogid", dialog_id);
            fragment.setArguments(bundle);
            ((LaunchActivity) parentActivity).presentFragment(fragment, "remind", false);
            //			}

        }

        return true;
    }

    private void meetingCall(String mid) {
        int chatId = 0;
        int userId = 0;
        if (currentChat != null)
            chatId = currentChat.id;
        if (currentUser != null)
            userId = currentUser.id;
        ArrayList<Integer> users = new ArrayList<Integer>();
        users.add(userId);
        MessagesController.getInstance().meetingCall(mid, chatId, users, 0);
        //锟斤拷锟斤拷锟斤拷锟绞撅拷锟较�
        String temp = LocaleController.getString("callingpeer", R.string.callingpeer);
        if (currentUser != null)
            MessagesController.getInstance().sendSystemMsg(currentUser.id, 0, temp, true);
        else
            MessagesController.getInstance().sendSystemMsg(0, currentChat.id, temp, true);

        //锟斤拷锟斤拷锟斤拷锟斤拷9锟剿ｏ拷锟斤拷锟斤拷锟斤拷锟斤拷硕锟斤拷校锟�1锟斤拷1锟皆讹拷锟斤拷锟皆和匡拷锟斤拷频锟斤拷锟斤拷锟剿碉拷锟斤拷要锟街讹拷锟斤拷锟斤拷
		/*if(currentChat!=null)
		{
			TLRPC.Chat chat = MessagesController.getInstance().chats.get(currentChat.id);
			if(chat!=null && chat.participants_count>ConstantValues.CALL_MAX_COUNT)
			{
				String alertMsg = String.format(ApplicationLoader.applicationContext.getString(R.string.MaxCount), ConstantValues.CALL_MAX_COUNT);
				Utilities.showToast(parentActivity, alertMsg);
				return;
			}
		}

		if (currentUser != null)
		{
			if( meeting.inMeeting && meeting.getUserId()!=currentUser.id )
			{
				UiUtil.showToastForChat(parentActivity, R.string.toast_msg_have_a_call);
				return;
			}
			// 锟斤拷示1锟斤拷1锟斤拷锟斤拷
			Intent intent = new Intent(parentActivity, PhoneActivity.class);
			Bundle bundle = new Bundle();
			String mid = "";
			if (currentUser.id < UserConfig.clientUserId)
				mid = "u" + currentUser.id + UserConfig.clientUserId;
			else
				mid = "u" + UserConfig.clientUserId + currentUser.id;
			bundle.putString("meetingId", mid);
			bundle.putInt("userId", currentUser.id);
			// 锟斤拷示锟斤拷锟斤拷锟斤拷锟斤拷
			bundle.putInt("callType", 1);
			intent.putExtras(bundle);
			startActivityForResult(intent, 100);
			//processSendingText("锟斤拷锟斤拷锟斤拷锟斤拷频通锟斤拷");
		}
		else if (currentChat != null)
		{
			if( meeting.inMeeting && meeting.getChatId()!=currentChat.id )
			{
				UiUtil.showToastForChat(parentActivity, R.string.toast_msg_have_a_call);
				return;
			}
			// 锟斤拷示群锟斤拷锟斤拷
			NotificationCenter.getInstance().addToMemCache(5, info);
			currentChat.incall=true;
			Intent intent = new Intent(parentActivity, PhoneActivity.class);
			Bundle bundle = new Bundle();
			String mid = "g" + currentChat.id;
			bundle.putString("meetingId", mid);
			bundle.putInt("chatId", currentChat.id);
			bundle.putInt("callType", 1);
			bundle.putInt("type", 1);
			intent.putExtras(bundle);
			startActivityForResult(intent, 100);
			//processSendingText("锟斤拷锟斤拷锟斤拷锟斤拷频通锟斤拷");
		}*/

    }

    public boolean isGoogleMapsInstalled() {
        return false;
        //		try {
        //			ApplicationInfo info = ApplicationLoader.applicationContext
        //					.getPackageManager().getApplicationInfo(
        //							"com.google.android.apps.maps", 0);
        //			return true;
        //		} catch (PackageManager.NameNotFoundException e) {
        //			AlertDialog.Builder builder = new AlertDialog.Builder(
        //					parentActivity);
        //			builder.setMessage("Install Google Maps?");
        //			builder.setCancelable(true);
        //			builder.setPositiveButton(
        //					LocaleController.getString("OK", R.string.OK),
        //					new DialogInterface.OnClickListener() {
        //						@Override
        //						public void onClick(DialogInterface dialogInterface,
        //								int i) {
        //							try {
        //								Intent intent = new Intent(
        //										Intent.ACTION_VIEW,
        //										Uri.parse("market://details?id=com.google.android.apps.maps"));
        //								startActivity(intent);
        //							} catch (Exception e) {
        //								FileLog.e("emm", e);
        //							}
        //						}
        //					});
        //			builder.setNegativeButton(R.string.Cancel, null);
        //			visibleDialog = builder.create();
        //			visibleDialog.setCanceledOnTouchOutside(true);
        //			visibleDialog.show();
        //			return false;
        //		}
    }

    public boolean isGooglePlayInstalled() {
        return false;
        //		int result = GooglePlayServicesUtil
        //				.isGooglePlayServicesAvailable(ApplicationLoader.applicationContext);
        //		if (result != ConnectionResult.SUCCESS) {
        //			Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(result,
        //					this.parentActivity, 0);
        //			int test = 0;
        //			int a = test;
        //			return false;
        //		}
        //		return false;
		/*
		 * try { ApplicationInfo info =
		 * ApplicationLoader.applicationContext.getPackageManager
		 * ().getApplicationInfo("com.google.android.gms", 0 ); return true; }
		 * catch(PackageManager.NameNotFoundException e) { AlertDialog.Builder
		 * builder = new AlertDialog.Builder(parentActivity);
		 * builder.setMessage("Install Google play?");
		 * builder.setCancelable(true);
		 * builder.setPositiveButton(LocaleController.getString("OK",
		 * R.string.OK), new DialogInterface.OnClickListener() {
		 *
		 * @Override public void onClick(DialogInterface dialogInterface, int i)
		 * { try { Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(
		 * "http://play.google.com/store/apps/details?id=com.google.android.gms"
		 * )); intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		 * intent.setPackage("com.android.vending"); startActivity(intent); }
		 * catch (ActivityNotFoundException e) { // Ok that didn't work, try the
		 * market method. try { Intent intent = new Intent(Intent.ACTION_VIEW,
		 * Uri.parse("market://details?id=com.google.android.gms"));
		 * intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		 * intent.setPackage("com.android.vending"); startActivity(intent); }
		 * catch (ActivityNotFoundException f) { // Ok, weird. Maybe they don't
		 * have any market app. Just show the website.
		 *
		 * Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(
		 * "http://play.google.com/store/apps/details?id=com.google.android.gms"
		 * )); intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		 * startActivity(intent); } } } });
		 * builder.setNegativeButton(R.string.Cancel, null); visibleDialog =
		 * builder.create(); visibleDialog.setCanceledOnTouchOutside(true);
		 * visibleDialog.show(); return false; }
		 */
    }

    private void updateVisibleRows() {
        if (chatListView == null) {
            return;
        }
        int count = chatListView.getChildCount();
        for (int a = 0; a < count; a++) {
            View view = chatListView.getChildAt(a);
            Object tag = view.getTag();
            if (tag instanceof ChatListRowHolderEx) {
                ChatListRowHolderEx holder = (ChatListRowHolderEx) tag;
                holder.update();

                boolean disableSelection = false;
                boolean selected = false;
                if (mActionMode != null) {
                    if (selectedMessagesIds
                            .containsKey(holder.message.messageOwner.id)) {
                        view.setBackgroundColor(0x6633b5e5);
                        selected = true;
                    } else {
                        view.setBackgroundColor(0);
                    }
                    disableSelection = true;
                } else {
                    view.setBackgroundColor(0);
                }
                updateRowBackground(holder, disableSelection, selected);
            } else if (view instanceof ChatBaseCell) {
                ChatBaseCell cell = (ChatBaseCell) view;

                boolean disableSelection = false;
                boolean selected = false;
                if (mActionMode != null) {
                    if (selectedMessagesIds
                            .containsKey(cell.getMessageObject().messageOwner.id)) {
                        view.setBackgroundColor(0x6633b5e5);
                        selected = true;
                    } else {
                        view.setBackgroundColor(0);
                    }
                    disableSelection = true;
                } else {
                    view.setBackgroundColor(0);
                }

                cell.setMessageObject(cell.getMessageObject());

                cell.setCheckPressed(!disableSelection, disableSelection
                        && selected);
            }
        }
    }

    /**
     * 锟斤拷锟斤拷谋锟斤拷锟酵计�
     *
     * @param holder
     * @param disableSelection
     * @param selected
     */
    private void updateRowBackground(ChatListRowHolderEx holder,
                                     boolean disableSelection, boolean selected) {
        int messageType = holder.message.type;
        if (!disableSelection) {
            if (messageType == 2 || messageType == 4 || messageType == 6) {
                holder.chatBubbleView
                        .setBackgroundResource(R.drawable.chat_outgoing_photo_states);
            } else if (messageType == 3 || messageType == 5 || messageType == 7) {
                holder.chatBubbleView
                        .setBackgroundResource(R.drawable.chat_incoming_photo_states);
            } else if (messageType == 12) {
                holder.chatBubbleView
                        .setBackgroundResource(R.drawable.chat_outgoing_text_states);
                holder.chatBubbleView.setPadding(Utilities.dp(6),
                        Utilities.dp(6), Utilities.dp(18), 0);
            } else if (messageType == 13) {
                holder.chatBubbleView
                        .setBackgroundResource(R.drawable.chat_incoming_text_states);
                holder.chatBubbleView.setPadding(Utilities.dp(15),
                        Utilities.dp(6), Utilities.dp(9), 0);
            } else if (messageType == 16) {
                holder.chatBubbleView
                        .setBackgroundResource(R.drawable.chat_outgoing_text_states);
                holder.chatBubbleView.setPadding(Utilities.dp(9),
                        Utilities.dp(9), Utilities.dp(18), 0);
            } else if (messageType == 17) {
                holder.chatBubbleView
                        .setBackgroundResource(R.drawable.chat_incoming_text_states);
                holder.chatBubbleView.setPadding(Utilities.dp(18),
                        Utilities.dp(9), Utilities.dp(9), 0);
            }
        } else {
            if (messageType == 2 || messageType == 4 || messageType == 6) {
                if (selected) {
                    holder.chatBubbleView
                            .setBackgroundResource(R.drawable.msg_out_photo_selected);
                } else {
                    holder.chatBubbleView
                            .setBackgroundResource(R.drawable.msg_out_photo);
                }
            } else if (messageType == 3 || messageType == 5 || messageType == 7) {
                if (selected) {
                    holder.chatBubbleView
                            .setBackgroundResource(R.drawable.msg_in_photo_selected);
                } else {
                    holder.chatBubbleView
                            .setBackgroundResource(R.drawable.msg_in_photo);
                }
            } else if (messageType == 12) {
                if (selected) {
                    holder.chatBubbleView
                            .setBackgroundResource(R.drawable.msg_out_selected);
                } else {
                    holder.chatBubbleView
                            .setBackgroundResource(R.drawable.msg_out);
                }
                holder.chatBubbleView.setPadding(Utilities.dp(6),
                        Utilities.dp(6), Utilities.dp(18), 0);
            } else if (messageType == 13) {
                if (selected) {
                    holder.chatBubbleView
                            .setBackgroundResource(R.drawable.msg_in_selected);
                } else {
                    holder.chatBubbleView
                            .setBackgroundResource(R.drawable.msg_in);
                }
                holder.chatBubbleView.setPadding(Utilities.dp(15),
                        Utilities.dp(6), Utilities.dp(9), 0);
            } else if (messageType == 16) {
                if (selected) {
                    holder.chatBubbleView
                            .setBackgroundResource(R.drawable.msg_out_selected);
                } else {
                    holder.chatBubbleView
                            .setBackgroundResource(R.drawable.msg_out);
                }
                holder.chatBubbleView.setPadding(Utilities.dp(9),
                        Utilities.dp(9), Utilities.dp(18), 0);
            } else if (messageType == 17) {
                if (selected) {
                    holder.chatBubbleView
                            .setBackgroundResource(R.drawable.msg_in_selected);
                } else {
                    holder.chatBubbleView
                            .setBackgroundResource(R.drawable.msg_in);
                }
                holder.chatBubbleView.setPadding(Utilities.dp(18),
                        Utilities.dp(9), Utilities.dp(9), 0);
            }
        }
    }

    //xueqiang 注锟斤拷
    private class ChatAdapter extends BaseAdapter {

        private Context mContext;

        public ChatAdapter(Context context) {
            mContext = context;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return true;
        }

        @Override
        public boolean isEnabled(int i) {
            return true;
        }

        @Override
        public int getCount() {
            int count = messages.size();
            if (count != 0) {
                if (!endReached) {
                    count++;
                }
                if (!unread_end_reached) {
                    count++;
                }
            }
            return count;
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            int offset = 1;
            if ((!endReached || !unread_end_reached) && messages.size() != 0) {
                if (!endReached) {
                    offset = 0;
                }
                if (i == 0 && !endReached || !unread_end_reached
                        && i == (messages.size() + 1 - offset)) {
                    if (view == null) {
                        LayoutInflater li = (LayoutInflater) mContext
                                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        view = li.inflate(R.layout.chat_loading_layout,
                                viewGroup, false);
                        View progressBar = view
                                .findViewById(R.id.progressLayout);
                        if (isCustomTheme) {
                            progressBar
                                    .setBackgroundResource(R.drawable.system_loader2);
                        } else {
                            progressBar
                                    .setBackgroundResource(R.drawable.system_loader1);
                        }
                    }
                    return view;
                }
            }
            // 锟斤拷录一锟斤拷锟斤拷messages锟叫碉拷锟斤拷锟斤拷位锟斤拷,only for audiocell使锟斤拷,xueqiang change
            int pos = messages.size() - i - offset;
            final MessageObject message = messages.get(pos);
            //Log.e("emm", "getview msg="+ message.messageText);
            int type = message.type;
            if (view == null) {
                LayoutInflater li = (LayoutInflater) mContext
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                if (type == 0) {
                    view = new ChatMessageCell(mContext, false);
                } else if (type == 1) {
                    view = new ChatMessageCell(mContext, currentChat != null);
                } else if (type == 8) {
                    view = new ChatMessageCell(mContext, false);
                } else if (type == 9) {
                    view = new ChatMessageCell(mContext, currentChat != null);
                } else if (type == 4) {
                    view = li.inflate(R.layout.chat_outgoing_location_layout,
                            viewGroup, false);
                } else if (type == 5) {
                    if (currentChat != null) {
                        view = li.inflate(
                                R.layout.chat_group_incoming_location_layout,//锟斤拷锟斤拷群锟斤拷图锟斤拷锟斤拷
                                viewGroup, false);
                    } else {
                        view = li.inflate(
                                R.layout.chat_incoming_location_layout,
                                viewGroup, false);
                    }
                } else if (type == 2) {
                    //锟斤拷锟斤拷图片锟侥诧拷锟斤拷
                    view = li.inflate(R.layout.chat_outgoing_photo_layout, viewGroup, false);
                } else if (type == 3) {
                    //锟斤拷锟斤拷图片锟侥诧拷锟斤拷
                    if (currentChat != null) {
                        view = li.inflate(R.layout.chat_group_incoming_photo_layout, viewGroup, false);//锟斤拷片

                    } else {
                        view = li.inflate(R.layout.chat_incoming_photo_layout, viewGroup, false);
                    }
                } else if (type == 6) {
                    view = li.inflate(R.layout.chat_outgoing_video_layout, viewGroup, false);//锟斤拷频
                } else if (type == 7) {
                    if (currentChat != null) {
                        view = li.inflate(R.layout.chat_group_incoming_video_layout, viewGroup, false);
                    } else {
                        view = li.inflate(R.layout.chat_incoming_video_layout, viewGroup, false);
                    }
                } else if (type == 10) {
                    view = li.inflate(R.layout.chat_action_message_layout, viewGroup, false);//锟斤拷息
                } else if (type == 11) {
                    view = li.inflate(R.layout.chat_action_change_photo_layout, viewGroup, false);
                } else if (type == 12) {
                    view = li.inflate(R.layout.chat_outgoing_contact_layout, viewGroup, false);
                } else if (type == 13) {
                    if (currentChat != null) {
                        view = li.inflate(R.layout.chat_group_incoming_contact_layout, viewGroup, false);
                    } else {
                        view = li.inflate(R.layout.chat_incoming_contact_layout, viewGroup, false);
                    }
                } else if (type == 15) {
                    view = li.inflate(R.layout.chat_unread_layout, viewGroup, false);
                } else if (type == 16) {
                    view = li.inflate(R.layout.chat_outgoing_document_layout, viewGroup, false);
                } else if (type == 17) {
                    if (currentChat != null) {
                        view = li.inflate(
                                R.layout.chat_group_incoming_document_layout,
                                viewGroup, false);
                    } else {
                        view = li.inflate(
                                R.layout.chat_incoming_document_layout,
                                viewGroup, false);
                    }
                } else if (type == 18) {
                    ChatAudioCell cell = new ChatAudioCell(mContext, false);
                    view = cell;
                } else if (type == 19) {
                    ChatAudioCell cell = new ChatAudioCell(mContext,
                            currentChat != null);
                    view = cell;
                } else if (type == 20) {
                    ChatReminderCell cell = new ChatReminderCell(mContext, false);
                    view = cell;
                    //					view = li.inflate(
                    //							R.layout.remind_msg_right_item,
                    //							viewGroup, false);
                } else if (type == 21) {
                    ChatReminderCell cell = new ChatReminderCell(mContext,
                            currentChat != null);
                    view = cell;
                    //					view = li.inflate(
                    //							R.layout.remind_msg_item,
                    //							viewGroup, false);

                }
                //XUEQIANG注锟斤拷锟角凤拷应锟斤拷锟斤拷写一锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷为20锟斤拷锟斤拷要锟角斤拷锟芥不同
            }

            if (view instanceof ChatBaseCell) {
                ((ChatBaseCell) view).delegate = new ChatBaseCell.ChatBaseCellDelegate() {
                    @Override
                    public void didPressedUserAvatar(ChatBaseCell cell,
                                                     TLRPC.User user) {
                        if (user != null && user.id != UserConfig.clientUserId) {
                            UserProfileActivity fragment = new UserProfileActivity();
                            Bundle args = new Bundle();
                            args.putInt("user_id", user.id);
                            fragment.setArguments(args);
                            ((LaunchActivity) parentActivity).presentFragment(
                                    fragment, "user_" + user.id, false);
                        }
                    }
                };
            }
            if (view instanceof ChatReminderCell) {
                ((ChatReminderCell) view).mEvent = new ChatReminderCell.MTouchEnevt() {
                    @Override
                    public void listenerTouch() {
                        selectMessage = message;
                        MessagesStorage.getInstance().getAlert(message.messageOwner.media.alert.guid);
                    }
                };
            }
            //			if (type == 20 || type == 21) {
            //				TextView tView = (TextView)view.findViewById(R.id.textView1);
            //				TextView tInfo = (TextView)view.findViewById(R.id.textView2);
            //				AlertMedia alert = message.messageOwner.media.alert;
            //				tInfo.setText(alert.msg);
            //				int titleId = 0;
            //				switch (message.subType) {
            //				case 0:
            //					titleId = R.string.remind_title_create;
            //					tView.setTextColor(Color.GRAY);
            //					break;
            //				case 1:
            //					titleId = R.string.remind_title_update;
            //					break;
            //				case 2:
            //					titleId = R.string.remind_title_delete;
            //					tView.setTextColor(Color.MAGENTA);
            //					break;
            //				default:
            //					break;
            //				}
            //				if (titleId != 0) {
            //					tView.setText(titleId);
            //				}
            //
            //				 view.findViewById(R.id.linlay).setOnClickListener(new OnClickListener() {
            //					@Override
            //					public void onClick(View v) {
            //						selectMessage = message;
            //						MessagesStorage.getInstance().getAlert(message.messageOwner.media.alert.guid);
            ////						RemindActivity fragment = new RemindActivity();
            ////						Bundle bundle = new Bundle();
            ////						bundle.putInt("userid", message.messageOwner.from_id);
            ////						bundle.putLong("dialogid", dialog_id);
            ////						bundle.putString("msg", message.messageOwner.media.alert.msg);
            ////						bundle.putInt("date", message.messageOwner.media.alert.date);
            ////						bundle.putInt("_status", message.messageOwner.media.alert._status);
            ////						bundle.putString("guid", message.messageOwner.media.alert.guid);
            ////						fragment.setArguments(bundle);
            ////						((LaunchActivity) parentActivity).presentFragment(fragment,
            ////								"remind", false);
            //					}
            //				});
            //
            //			}
            boolean selected = false;
            boolean disableSelection = false;
            if (mActionMode != null) {
                if (selectedMessagesIds.containsKey(message.messageOwner.id)) {
                    view.setBackgroundColor(0x6633b5e5);
                    selected = true;
                } else {
                    view.setBackgroundColor(0);
                }
                disableSelection = true;
            } else {
                view.setBackgroundColor(0);
            }

            if (view instanceof ChatBaseCell) {
                ((ChatBaseCell) view).setMessageObject(message);
                ((ChatBaseCell) view).setCheckPressed(!disableSelection, disableSelection && selected);
                if (view instanceof ChatAudioCell && downloadAudios) {
                    ((ChatAudioCell) view).downloadAudioIfNeed();
                }
            } else {
                ChatListRowHolderEx holder = (ChatListRowHolderEx) view.getTag();
                if (holder == null) {
                    holder = new ChatListRowHolderEx(view, type);
                    view.setTag(holder);
                }
                //锟斤拷示锟斤拷锟斤拷图片
                holder.message = message;
                updateRowBackground(holder, disableSelection, selected);
                holder.update();
            }

            return view;
        }

        @Override
        public int getItemViewType(int i) {
            int offset = 1;
            if (!endReached && messages.size() != 0) {
                offset = 0;
                if (i == 0) {
                    return 14;
                }
            }
            if (!unread_end_reached && i == (messages.size() + 1 - offset)) {
                return 14;
            }
            MessageObject message = messages.get(messages.size() - i - offset);
            return message.type;
        }

        @Override
        public int getViewTypeCount() {
            return 22;
        }

        @Override
        public boolean isEmpty() {
            int count = messages.size();
            if (count != 0) {
                if (!endReached) {
                    count++;
                }
                if (!unread_end_reached) {
                    count++;
                }
            }
            return count == 0;
        }
    }

    public class ChatListRowHolderEx {
        public BackupImageView avatarImageView;

        public TextView nameTextView;

        public TextView messageTextView;

        public MessageActionLayout messageLayoutAction;

        public TextView timeTextView;

        public BackupImageView photoImage;

        public ImageView halfCheckImage;

        public ImageView checkImage;

        public TextView actionAttachButton;

        public TextView videoTimeText;

        public MessageObject message;

        public TextView phoneTextView;

        public BackupImageView contactAvatar;

        public View contactView;

        public ImageView addContactButton;

        public View addContactView;

        public View chatBubbleView;

        public View photoProgressView;

        public ProgressBar actionProgress;

        public View actionView;

        public ImageView actionCancelButton;

        public TextView userNameTextView;

        private PhotoObject photoObjectToSet = null;

        private File photoFile = null;

        private String photoFileName = null;

        private String photoFilter = null;

        public void update() {
            TLRPC.User fromUser = MessagesController.getInstance().users
                    .get(message.messageOwner.from_id);


            int type = message.type;

            if (timeTextView != null) {
                timeTextView.setText(LocaleController.formatterDay
                        .format((long) (message.messageOwner.date) * 1000));
            }

            if (userNameTextView != null) {
                String userName = Utilities.formatName(fromUser);
                userNameTextView.setText(userName);
            }

            if (avatarImageView != null && fromUser != null) {
                TLRPC.FileLocation photo = null;
                if (fromUser.photo != null) {
                    photo = fromUser.photo.photo_small;
                }
                int placeHolderId = Utilities.getUserAvatarForId(fromUser.id);
                avatarImageView.setImage(photo, "50_50", placeHolderId);
            }

            if (type != 12 && type != 13 && nameTextView != null
                    && fromUser != null && type != 16 && type != 17) {
                String nameString = Utilities.formatName(fromUser);
                nameTextView.setText(nameString);
                nameTextView.setTextColor(Utilities
                        .getColorForId(message.messageOwner.from_id));
            }

            if (type == 2 || type == 3 || type == 6 || type == 7) {
                int width = (int) (Math.min(displaySize.x, displaySize.y) * 0.7f);
                int height = width + Utilities.dp(100);
                if (type == 6 || type == 7) {
                    width = (int) (Math.min(displaySize.x, displaySize.y) / 2.5f);
                    height = width + 100;
                }
                if (width > 800) {
                    width = 800;
                }
                if (height > 800) {
                    height = 800;
                }

                PhotoObject photo = PhotoObject.getClosestImageWithSize(
                        message.photoThumbs, width, height);
                if (type == 3) {
                    if (photoProgressView != null) {
                        photoProgressView.setVisibility(View.GONE);
                    }
                }

                if (photo != null) {
                    float scale = (float) photo.photoOwner.w / (float) width;

                    int w = (int) (photo.photoOwner.w / scale);
                    int h = (int) (photo.photoOwner.h / scale);
                    if (h > height) {
                        float scale2 = h;
                        h = height;
                        scale2 /= h;
                        w = (int) (w / scale2);
                    } else if (h < Utilities.dp(120)) {
                        h = Utilities.dp(120);
                        float hScale = (float) photo.photoOwner.h / h;
                        if (photo.photoOwner.w / hScale < width) {
                            w = (int) (photo.photoOwner.w / hScale);
                        }
                    }

                    FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) photoImage
                            .getLayoutParams();
                    params.width = w;
                    params.height = h;
                    photoImage.setLayoutParams(params);

                    LinearLayout.LayoutParams params2 = (LinearLayout.LayoutParams) chatBubbleView
                            .getLayoutParams();
                    params2.width = w + Utilities.dp(12);
                    params2.height = h + Utilities.dp(12);
                    chatBubbleView.setLayoutParams(params2);

                    if (photo.image != null) {
                        photoImage.setImageBitmap(photo.image);
                    } else {
                        if (type == 2 || type == 3) {
                            String fileName = MessageObject
                                    .getAttachFileName(photo.photoOwner);
                            String path = Utilities.getCacheDir()
                                    .getAbsolutePath();
                            File cacheFile = new File(Utilities.getCacheDir(),
                                    fileName);
                            // FileLog.e("emm","filename="+fileName);
                            if (!cacheFile.exists()) {
                                photoFileName = fileName;
                                photoFile = cacheFile;
                                // FileLog.e("emm","filename can't exist="+fileName);
                            } else {
                                photoFileName = null;
                                photoFile = null;
                                // FileLog.e("emm","filename  exist="+fileName);
                            }
                        }
                        if (photoFileName == null) {
                            if (message.imagePreview != null) {
                                //锟斤拷锟斤拷锟斤拷氐锟斤拷锟�90x90锟斤拷小图片锟斤拷然锟斤拷锟斤拷锟絧hotoImage.setImage锟接憋拷锟截硷拷锟截达拷锟酵计�
                                //注锟斤拷锟角达拷fileloader.loadimage锟叫硷拷锟截的达拷图片
                                int w1 = message.imagePreview.getWidth();
                                int h1 = message.imagePreview.getHeight();
                                photoImage.setImage(photo.photoOwner.location,
                                        String.format(Locale.US, "%d_%d",
                                                (int) (w / Utilities.density),
                                                (int) (h / Utilities.density)),
                                        message.imagePreview);
                            } else {
                                photoImage
                                        .setImage(
                                                photo.photoOwner.location,
                                                String.format(
                                                        Locale.US,
                                                        "%d_%d",
                                                        (int) (w / Utilities.density),
                                                        (int) (h / Utilities.density)),
                                                message.messageOwner.out ? R.drawable.photo_placeholder_out
                                                        : R.drawable.photo_placeholder_in);
                            }
                        } else {
                            if (downloadPhotos) {
                                //xueqiang 锟斤拷锟斤拷锟斤拷示图片锟侥达拷锟斤拷
                                addToLoadingFile(photoFileName, actionProgress);
                                if (message.imagePreview != null) {
                                    photoImage
                                            .setImage(
                                                    photo.photoOwner.location,
                                                    String.format(
                                                            Locale.US,
                                                            "%d_%d",
                                                            (int) (w / Utilities.density),
                                                            (int) (h / Utilities.density)),
                                                    message.imagePreview,
                                                    photo.photoOwner.size);
                                } else {
                                    //锟斤拷示图片锟斤拷锟斤拷锟斤拷锟斤拷锟矫伙拷校锟斤拷锟斤拷源锟斤拷锟斤拷锟斤拷匣锟饺�
                                    photoImage
                                            .setImage(
                                                    photo.photoOwner.location,
                                                    String.format(
                                                            Locale.US,
                                                            "%d_%d",
                                                            (int) (w / Utilities.density),
                                                            (int) (h / Utilities.density)),
                                                    message.messageOwner.out ? R.drawable.photo_placeholder_out
                                                            : R.drawable.photo_placeholder_in,
                                                    photo.photoOwner.size);
                                }
                                photoObjectToSet = null;
                                photoFilter = null;
                            } else {
                                //								FileLog.e("emm", "filename  downloadPhotos 2");
                                photoFilter = String.format(Locale.US, "%d_%d",
                                        (int) (w / Utilities.density),
                                        (int) (h / Utilities.density));
                                photoObjectToSet = photo;
                                photoImage.setImageBitmap(message.imagePreview);
                            }
                        }
                    }
                }

                if ((type == 6 || type == 7) && videoTimeText != null) {
                    int duration = message.messageOwner.media.video.duration;
                    int minutes = duration / 60;
                    int seconds = duration - minutes * 60;
                    videoTimeText.setText(String.format("%d:%02d", minutes,
                            seconds));
                }
            } else if (type == 4 || type == 5) {
                double lat = message.messageOwner.media.geo.lat;
                double lon = message.messageOwner.media.geo._long;
                // jenf for location
                String url = "";
                if (LocaleController.getInstance().IsChineseMainLand()) {
                    url = String
                            .format(Locale.SIMPLIFIED_CHINESE,
                                    "http://restapi.amap.com/v3/staticmap?location=%f,%f&zoom=14&size=100*100&markers=mid,,:%f,%f&key=869c21e6435df4a23299d29396d4a9eb",
                                    lon, lat, lon, lat);
                } else {
                    url = String
                            .format(Locale.US,
                                    "https://maps.googleapis.com/maps/api/staticmap?center=%f,%f&zoom=13&size=100x100&maptype=roadmap&scale=%d&markers=color:red|size:big|%f,%f&sensor=false",
                                    lat,
                                    lon,
                                    Math.min(2,
                                            (int) Math.ceil(Utilities.density)),
                                    lat, lon);
                }
                photoImage
                        .setImage(
                                url,
                                null,
                                message.messageOwner.out ? R.drawable.photo_placeholder_out
                                        : R.drawable.photo_placeholder_in);
                actionAttachButton.setText(LocaleController.getString(
                        "ViewLocation", R.string.ViewLocation));
            } else if (type == 11 || type == 10) {
                int width = displaySize.x - Utilities.dp(30);
                Log.e("emm", "holder update msg=" + message.messageText + "");
                messageTextView.setText(message.messageText);
                messageTextView.setMaxWidth(width);

                if (type == 11) {
                    if (message.messageOwner.action instanceof TLRPC.TL_messageActionUserUpdatedPhoto) {
                        photoImage
                                .setImage(
                                        message.messageOwner.action.newUserPhoto.photo_small,
                                        "50_50",
                                        Utilities
                                                .getUserAvatarForId(currentUser.id));
                    } else {
                        PhotoObject photo = PhotoObject
                                .getClosestImageWithSize(message.photoThumbs,
                                        Utilities.dp(64), Utilities.dp(64));
                        if (photo.image != null) {
                            photoImage.setImageBitmap(photo.image);
                        } else {
                            photoImage
                                    .setImage(
                                            photo.photoOwner.location,
                                            "50_50",
                                            Utilities
                                                    .getGroupAvatarForId(currentChat.id));
                        }
                    }
                }
            } else if (type == 12 || type == 13) {
                TLRPC.User contactUser = MessagesController.getInstance().users
                        .get(message.messageOwner.media.user_id);
                if (contactUser != null) {
                    nameTextView.setText(Utilities.formatName(
                            message.messageOwner.media.first_name,
                            message.messageOwner.media.last_name));
                    nameTextView.setTextColor(Utilities
                            .getColorForId(contactUser.id));
                    String phone = message.messageOwner.media.phone_number;
                    if (phone != null && phone.length() != 0) {
                        if (!phone.startsWith("+")) {
                            phone = "+" + phone;
                        }
                        phoneTextView.setText(PhoneFormat.getInstance().format(
                                phone));
                    } else {
                        phoneTextView.setText("Unknown");
                    }
                    TLRPC.FileLocation photo = null;
                    if (contactUser.photo != null) {
                        photo = contactUser.photo.photo_small;
                    }
                    int placeHolderId = Utilities
                            .getUserAvatarForId(contactUser.id);
                    contactAvatar.setImage(photo, "50_50", placeHolderId);
                    if (contactUser.id != UserConfig.clientUserId
                            && ContactsController.getInstance().contactsDict
                            .get(contactUser.id) == null) {
                        addContactView.setVisibility(View.VISIBLE);
                    } else {
                        addContactView.setVisibility(View.GONE);
                    }
                } else {
                    nameTextView.setText(Utilities.formatName(
                            message.messageOwner.media.first_name,
                            message.messageOwner.media.last_name));
                    nameTextView.setTextColor(Utilities
                            .getColorForId(message.messageOwner.media.user_id));
                    String phone = message.messageOwner.media.phone_number;
                    if (phone != null && phone.length() != 0) {
                        if (message.messageOwner.media.user_id != 0
                                && !phone.startsWith("+")) {
                            phone = "+" + phone;
                        }
                        phoneTextView.setText(PhoneFormat.getInstance().format(
                                phone));
                    } else {
                        phoneTextView.setText("Unknown");
                    }
                    contactAvatar
                            .setImageResource(Utilities
                                    .getUserAvatarForId(message.messageOwner.media.user_id));
                    addContactView.setVisibility(View.GONE);
                }
            } else if (type == 15) {
                // sam
                if (unread_to_load != 0) {
                    if (unread_to_load == 1) {
                        messageTextView.setText(LocaleController.formatString(
                                "OneNewMessage", R.string.OneNewMessage,
                                unread_to_load));
                    } else {
                        messageTextView.setText(LocaleController.formatString(
                                "FewNewMessages", R.string.FewNewMessages,
                                unread_to_load));
                    }
                }
            } else if (type == 16 || type == 17) {
                TLRPC.Document document = message.messageOwner.media.document;
                if (document instanceof TLRPC.TL_document
                        || document instanceof TLRPC.TL_documentEncrypted) {
                    nameTextView
                            .setText(message.messageOwner.media.document.file_name);

                    String fileName = message.getFileName();
                    int idx = fileName.lastIndexOf(".");
                    String ext = null;
                    if (idx != -1) {
                        ext = fileName.substring(idx + 1);
                    }
                    if (ext == null || ext.length() == 0) {
                        ext = message.messageOwner.media.document.mime_type;
                    }
                    ext = ext.toUpperCase();
                    if (document.size < 1024) {
                        phoneTextView.setText(String.format("%d B %s",
                                document.size, ext));
                    } else if (document.size < 1024 * 1024) {
                        phoneTextView.setText(String.format("%.1f KB %s",
                                document.size / 1024.0f, ext));
                    } else {
                        phoneTextView.setText(String.format("%.1f MB %s",
                                document.size / 1024.0f / 1024.0f, ext));
                    }
                    if (document.thumb instanceof TLRPC.TL_photoSize) {

                    } else if (document.thumb instanceof TLRPC.TL_photoCachedSize) {

                    } else {
                        if (type == 16) {
                            contactAvatar
                                    .setImageResource(R.drawable.doc_green);
                        } else {
                            contactAvatar.setImageResource(R.drawable.doc_blue);
                        }
                    }
                } else {
                    nameTextView.setText("Error");
                    phoneTextView.setText("Error");
                    if (type == 16) {
                        contactAvatar.setImageResource(R.drawable.doc_green);
                    } else {
                        contactAvatar.setImageResource(R.drawable.doc_blue);
                    }
                }
            }

            if (message.messageOwner.id < 0
                    && message.messageOwner.send_state != MessagesController.MESSAGE_SEND_STATE_SEND_ERROR
                    && message.messageOwner.send_state != MessagesController.MESSAGE_SEND_STATE_SENT) {
                if (MessagesController.getInstance().sendingMessages
                        .get(message.messageOwner.id) == null) {
                    message.messageOwner.send_state = MessagesController.MESSAGE_SEND_STATE_SEND_ERROR;
                }
            }

            if (message.messageOwner.from_id == UserConfig.clientUserId) {
                if (halfCheckImage != null) {
                    if (message.messageOwner.send_state == MessagesController.MESSAGE_SEND_STATE_SENDING) {
                        checkImage.setVisibility(View.INVISIBLE);
                        if (type == 2 || type == 6 || type == 4) {
                            halfCheckImage
                                    .setImageResource(R.drawable.msg_clock_photo);
                        } else {
                            halfCheckImage
                                    .setImageResource(R.drawable.msg_clock);
                        }
                        halfCheckImage.setVisibility(View.VISIBLE);
                        if (actionView != null || photoProgressView != null) {
                            if (actionView != null) {
                                actionView.setVisibility(View.VISIBLE);
                            }
                            if (photoProgressView != null) {
                                photoProgressView.setVisibility(View.VISIBLE);
                            }
                            Float progress = FileLoader.getInstance().fileProgresses
                                    .get(message.messageOwner.attachPath);
                            if (progress != null) {
                                actionProgress
                                        .setProgress((int) (progress * 100));
                            } else {
                                actionProgress.setProgress(0);
                            }
                            progressByTag.put(
                                    (Integer) actionProgress.getTag(),
                                    message.messageOwner.attachPath);
                            progressBarMap.put(message.messageOwner.attachPath,
                                    actionProgress);
                        }
                        if (actionAttachButton != null) {
                            actionAttachButton.setVisibility(View.GONE);
                        }
                    } else if (message.messageOwner.send_state == MessagesController.MESSAGE_SEND_STATE_SEND_ERROR) {
                        halfCheckImage.setVisibility(View.VISIBLE);
                        halfCheckImage.setImageResource(R.drawable.msg_warning);
                        if (checkImage != null) {
                            checkImage.setVisibility(View.INVISIBLE);
                        }
                        if (actionView != null) {
                            actionView.setVisibility(View.GONE);
                        }
                        if (photoProgressView != null) {
                            photoProgressView.setVisibility(View.GONE);
                        }
                        if (actionAttachButton != null) {
                            actionAttachButton.setVisibility(View.GONE);
                        }
                    } else if (message.messageOwner.send_state == MessagesController.MESSAGE_SEND_STATE_SENT) {
                        if (!message.messageOwner.unread) {
                            halfCheckImage.setVisibility(View.VISIBLE);
                            checkImage.setVisibility(View.VISIBLE);
                            if (type == 2 || type == 6 || type == 4) {
                                halfCheckImage
                                        .setImageResource(R.drawable.msg_halfcheck_w);
                            } else {
                                halfCheckImage
                                        .setImageResource(R.drawable.msg_halfcheck);
                            }
                        } else {
                            halfCheckImage.setVisibility(View.VISIBLE);
                            checkImage.setVisibility(View.INVISIBLE);
                            if (type == 2 || type == 6 || type == 4) {
                                halfCheckImage
                                        .setImageResource(R.drawable.msg_check_w);
                            } else {
                                halfCheckImage
                                        .setImageResource(R.drawable.msg_check);
                            }
                        }
                        if (actionView != null) {
                            actionView.setVisibility(View.GONE);
                        }
                        if (photoProgressView != null) {
                            photoProgressView.setVisibility(View.GONE);
                        }
                        if (actionAttachButton != null) {
                            actionAttachButton.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }
            if (message.type == 2 || message.type == 3 || message.type == 6
                    || message.type == 7 || message.type == 16
                    || message.type == 17) {
                Integer tag = (Integer) actionProgress.getTag();
                String file = progressByTag.get(tag);
                if (file != null) {
                    removeFromloadingFile(file, actionProgress);
                }
                if (message.messageOwner.send_state != MessagesController.MESSAGE_SEND_STATE_SENDING
                        && message.messageOwner.send_state != MessagesController.MESSAGE_SEND_STATE_SEND_ERROR) {
                    if (file != null) {
                        progressBarMap.remove(file);
                    }
                    String fileName = null;
                    if (photoFileName != null) {
                        fileName = photoFileName;
                    } else {
                        fileName = message.getFileName();
                    }
                    boolean load = false;
                    if (message.type != 2 && message.type != 3
                            && message.messageOwner.attachPath != null
                            && message.messageOwner.attachPath.length() != 0) {
                        File f = new File(message.messageOwner.attachPath);
                        if (f.exists()) {
                            if (actionAttachButton != null) {
                                actionAttachButton.setVisibility(View.VISIBLE);
                                if (message.type == 6 || message.type == 7) {
                                    actionAttachButton.setText(LocaleController
                                            .getString("ViewVideo",
                                                    R.string.ViewVideo));
                                } else if (message.type == 16
                                        || message.type == 17) {
                                    actionAttachButton.setText(LocaleController
                                            .getString("Open", R.string.Open));
                                }
                            }
                            if (actionView != null) {
                                actionView.setVisibility(View.GONE);
                            }
                            if (photoProgressView != null) {
                                photoProgressView.setVisibility(View.GONE);
                            }
                        } else {
                            load = true;
                        }
                    }
                    if (load
                            && message.messageOwner.attachPath != null
                            && message.messageOwner.attachPath.length() != 0
                            || !load
                            && (message.messageOwner.attachPath == null || message.messageOwner.attachPath
                            .length() == 0)) {
                        File cacheFile = null;
                        if (((message.type == 2 || message.type == 3) && photoFileName == null)
                                || (cacheFile = new File(
                                Utilities.getCacheDir(), fileName))
                                .exists()) {
                            if (actionAttachButton != null) {
                                actionAttachButton.setVisibility(View.VISIBLE);
                                if (message.type == 6 || message.type == 7) {
                                    actionAttachButton.setText(LocaleController
                                            .getString("ViewVideo",
                                                    R.string.ViewVideo));
                                } else if (message.type == 16
                                        || message.type == 17) {
                                    actionAttachButton.setText(LocaleController
                                            .getString("Open", R.string.Open));
                                }
                            }
                            if (actionView != null) {
                                actionView.setVisibility(View.GONE);
                            }
                            if (photoProgressView != null) {
                                photoProgressView.setVisibility(View.GONE);
                            }
                            load = false;
                        } else {
                            load = true;
                        }
                    }
                    if (load) {
                        Float progress = FileLoader.getInstance().fileProgresses
                                .get(fileName);
                        if (loadingFile.containsKey(fileName)
                                || progress != null) {
                            if (progress != null) {
                                actionProgress
                                        .setProgress((int) (progress * 100));
                            } else {
                                actionProgress.setProgress(0);
                            }
                            progressByTag
                                    .put((Integer) actionProgress.getTag(),
                                            fileName);
                            addToLoadingFile(fileName, actionProgress);
                            if (actionView != null) {
                                if ((message.type == 2 || message.type == 3)
                                        && downloadPhotos) {
                                    actionView.setVisibility(View.GONE);
                                } else {
                                    actionView.setVisibility(View.VISIBLE);
                                    if (photoFileName != null) {
                                        actionCancelButton
                                                .setImageResource(R.drawable.photo_download_cancel_states);
                                    }
                                }
                            }
                            if (photoProgressView != null) {
                                photoProgressView.setVisibility(View.VISIBLE);
                            }
                            if (actionAttachButton != null) {
                                actionAttachButton.setVisibility(View.GONE);
                            }
                        } else {
                            if (actionView != null) {
                                if ((message.type == 2 || message.type == 3)
                                        && !downloadPhotos) {
                                    actionView.setVisibility(View.VISIBLE);
                                    actionCancelButton
                                            .setImageResource(R.drawable.photo_download_states);
                                } else {
                                    actionView.setVisibility(View.GONE);
                                }
                            }
                            if (photoProgressView != null) {
                                photoProgressView.setVisibility(View.GONE);
                            }
                            if (actionAttachButton != null) {
                                actionAttachButton.setVisibility(View.VISIBLE);
                                if (message.type == 6 || message.type == 7) {
                                    actionAttachButton
                                            .setText(String
                                                    .format("%s %.1f MB",
                                                            LocaleController
                                                                    .getString(
                                                                            "DOWNLOAD",
                                                                            R.string.DOWNLOAD),
                                                            message.messageOwner.media.video.size / 1024.0f / 1024.0f));
                                } else if (message.type == 16
                                        || message.type == 17) {
                                    actionAttachButton.setText(LocaleController
                                            .getString("DOWNLOAD",
                                                    R.string.DOWNLOAD));
                                }
                            }
                        }
                    }
                }
                if (message.type == 16 || message.type == 17) {
                    int width;
                    if (currentChat != null && type != 16) {
                        if (actionView.getVisibility() == View.VISIBLE) {
                            width = displaySize.x - Utilities.dp(290);
                        } else {
                            width = displaySize.x - Utilities.dp(270);
                        }
                    } else {
                        if (actionView.getVisibility() == View.VISIBLE) {
                            width = displaySize.x - Utilities.dp(240);
                        } else {
                            width = displaySize.x - Utilities.dp(220);
                        }
                    }
                    nameTextView.setMaxWidth(width);
                    phoneTextView.setMaxWidth(width);
                }
            }
        }

        public ChatListRowHolderEx(View view, int type) {
            avatarImageView = (BackupImageView) view
                    .findViewById(R.id.chat_group_avatar_image);
            nameTextView = (TextView) view
                    .findViewById(R.id.chat_user_group_name);
            messageLayoutAction = (MessageActionLayout) view
                    .findViewById(R.id.message_action_layout);
            timeTextView = (TextView) view.findViewById(R.id.chat_time_text);
            userNameTextView = (TextView) view
                    .findViewById(R.id.chat_username_text);
            photoImage = (BackupImageView) view
                    .findViewById(R.id.chat_photo_image);
            halfCheckImage = (ImageView) view
                    .findViewById(R.id.chat_row_halfcheck);
            checkImage = (ImageView) view.findViewById(R.id.chat_row_check);
            actionAttachButton = (TextView) view
                    .findViewById(R.id.chat_view_action_button);
            messageTextView = (TextView) view
                    .findViewById(R.id.chat_message_text);
            videoTimeText = (TextView) view.findViewById(R.id.chat_video_time);
            actionView = view.findViewById(R.id.chat_view_action_layout);
            actionProgress = (ProgressBar) view
                    .findViewById(R.id.chat_view_action_progress);
            actionCancelButton = (ImageView) view
                    .findViewById(R.id.chat_view_action_cancel_button);
            phoneTextView = (TextView) view.findViewById(R.id.phone_text_view);
            contactAvatar = (BackupImageView) view
                    .findViewById(R.id.contact_avatar);
            contactView = view.findViewById(R.id.shared_layout);
            addContactButton = (ImageView) view
                    .findViewById(R.id.add_contact_button);
            addContactView = view.findViewById(R.id.add_contact_view);
            chatBubbleView = view.findViewById(R.id.chat_bubble_layout);
            photoProgressView = view.findViewById(R.id.photo_progress);
            if (messageTextView != null) {
                messageTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP,
                        MessagesController.getInstance().fontSize);
            }

            if (actionProgress != null) {
                actionProgress.setTag(progressTag);
                progressTag++;
            }

            if (type != 2 && type != 3) {
                if (actionView != null) {
                    if (isCustomTheme) {
                        actionView
                                .setBackgroundResource(R.drawable.system_black);
                    } else {
                        actionView
                                .setBackgroundResource(R.drawable.system_blue);
                    }
                }
            }

            if (messageLayoutAction != null) {
                if (isCustomTheme) {
                    messageLayoutAction
                            .setBackgroundResource(R.drawable.system_black);
                } else {
                    messageLayoutAction
                            .setBackgroundResource(R.drawable.system_blue);
                }
            }

            if (addContactButton != null) {
                addContactButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mActionMode != null) {
                            processRowSelect(view);
                            return;
                        }
                        ContactAddActivity fragment = new ContactAddActivity();
                        Bundle args = new Bundle();
                        args.putInt("user_id",
                                message.messageOwner.media.user_id);
                        args.putString("phone",
                                message.messageOwner.media.phone_number);
                        fragment.setArguments(args);
                        ((LaunchActivity) parentActivity).presentFragment(
                                fragment, "add_contact_"
                                        + message.messageOwner.media.user_id,
                                false);
                    }
                });

                addContactButton
                        .setOnLongClickListener(new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View v) {
                                createMenu(v, false);
                                return true;
                            }
                        });
            }

            if (contactView != null) {
                contactView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (message.type == 16 || message.type == 17) {
                            processOnClick(view);
                        } else if (message.type == 12 || message.type == 13) {
                            if (mActionMode != null) {
                                processRowSelect(view);
                                return;
                            }
                            if (message.messageOwner.media.user_id != UserConfig.clientUserId) {
                                TLRPC.User user = null;
                                if (message.messageOwner.media.user_id != 0) {
                                    user = MessagesController.getInstance().users
                                            .get(message.messageOwner.media.user_id);
                                }
                                if (user != null) {
                                    UserProfileActivity fragment = new UserProfileActivity();
                                    Bundle args = new Bundle();
                                    args.putInt("user_id",
                                            message.messageOwner.media.user_id);
                                    fragment.setArguments(args);
                                    ((LaunchActivity) parentActivity)
                                            .presentFragment(
                                                    fragment,
                                                    "user_"
                                                            + message.messageOwner.media.user_id,
                                                    false);
                                } else {
                                    if (parentActivity == null
                                            || message.messageOwner.media.phone_number == null
                                            || message.messageOwner.media.phone_number
                                            .length() == 0) {
                                        return;
                                    }
                                    AlertDialog.Builder builder = new AlertDialog.Builder(
                                            parentActivity);
                                    builder.setItems(
                                            new CharSequence[]{
                                                    LocaleController.getString(
                                                            "Copy",
                                                            R.string.Copy),
                                                    LocaleController.getString(
                                                            "Call",
                                                            R.string.Call)},
                                            new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(
                                                        DialogInterface dialogInterface,
                                                        int i) {
                                                    if (i == 1) {
                                                        try {
                                                            Intent intent = new Intent(
                                                                    Intent.ACTION_DIAL,
                                                                    Uri.parse("tel:"
                                                                            + message.messageOwner.media.phone_number));
                                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                            startActivity(intent);
                                                        } catch (Exception e) {
                                                            FileLog.e("emm", e);
                                                        }
                                                    } else if (i == 0) {
                                                        ActionBarActivity inflaterActivity = parentActivity;
                                                        if (inflaterActivity == null) {
                                                            inflaterActivity = (ActionBarActivity) getActivity();
                                                        }
                                                        if (inflaterActivity == null) {
                                                            return;
                                                        }
                                                        int sdk = android.os.Build.VERSION.SDK_INT;
                                                        if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
                                                            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) inflaterActivity
                                                                    .getSystemService(Context.CLIPBOARD_SERVICE);
                                                            clipboard
                                                                    .setText(message.messageOwner.media.phone_number);
                                                        } else {
                                                            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) inflaterActivity
                                                                    .getSystemService(Context.CLIPBOARD_SERVICE);
                                                            android.content.ClipData clip = android.content.ClipData
                                                                    .newPlainText(
                                                                            "label",
                                                                            message.messageOwner.media.phone_number);
                                                            clipboard
                                                                    .setPrimaryClip(clip);
                                                        }
                                                    }
                                                }
                                            });
                                    visibleDialog = builder.show();
                                    visibleDialog
                                            .setCanceledOnTouchOutside(true);
                                    visibleDialog
                                            .setOnDismissListener(new DialogInterface.OnDismissListener() {
                                                @Override
                                                public void onDismiss(
                                                        DialogInterface dialog) {
                                                    visibleDialog = null;
                                                }
                                            });
                                }
                            }
                        }
                    }
                });

                contactView
                        .setOnLongClickListener(new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View v) {
                                createMenu(v, false);
                                return true;
                            }
                        });
            }

            if (contactAvatar != null) {
                contactAvatar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (message.type == 18 || message.type == 19) {
                            if (message.messageOwner.media.audio.user_id != UserConfig.clientUserId
                                    && message.messageOwner.media.audio.user_id != 0) {
                                UserProfileActivity fragment = new UserProfileActivity();
                                Bundle args = new Bundle();
                                args.putInt(
                                        "user_id",
                                        message.messageOwner.media.audio.user_id);
                                fragment.setArguments(args);
                                ((LaunchActivity) parentActivity)
                                        .presentFragment(
                                                fragment,
                                                "user_"
                                                        + message.messageOwner.media.audio.user_id,
                                                false);
                            }
                        }
                    }
                });
            }

            if (actionAttachButton != null) {
                actionAttachButton
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                processOnClick(view);
                            }
                        });
            }

            if (avatarImageView != null) {
                avatarImageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mActionMode != null) {
                            processRowSelect(view);
                            return;
                        }
                        if (message != null) {
                            UserProfileActivity fragment = new UserProfileActivity();
                            Bundle args = new Bundle();
                            args.putInt("user_id", message.messageOwner.from_id);
                            fragment.setArguments(args);
                            ((LaunchActivity) parentActivity).presentFragment(
                                    fragment, "user_"
                                            + message.messageOwner.from_id,
                                    false);
                        }
                    }
                });
            }

            if (actionCancelButton != null) {
                actionCancelButton
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (message != null) {
                                    Integer tag = (Integer) actionProgress
                                            .getTag();
                                    if (message.messageOwner.send_state != 0) {
                                        MessagesController.getInstance()
                                                .cancelSendingMessage(message);
                                        String file = progressByTag.get(tag);
                                        if (file != null) {
                                            progressBarMap.remove(file);
                                        }
                                    } else if (message.type == 6
                                            || message.type == 7
                                            || message.type == 16
                                            || message.type == 17) {
                                        String file = progressByTag.get(tag);
                                        if (file != null) {
                                            loadingFile.remove(file);
                                            if (message.type == 6
                                                    || message.type == 7) {
                                                FileLoader
                                                        .getInstance()
                                                        .cancelLoadFile(
                                                                message.messageOwner.media.video,
                                                                null, null,
                                                                null);
                                            } else if (message.type == 16
                                                    || message.type == 17) {
                                                FileLoader
                                                        .getInstance()
                                                        .cancelLoadFile(
                                                                null,
                                                                null,
                                                                message.messageOwner.media.document,
                                                                null);
                                            }
                                            updateVisibleRows();
                                        }
                                    } else if (message.type == 2
                                            || message.type == 3) {
                                        if (photoFile != null
                                                && !photoFile.exists()
                                                && photoObjectToSet != null) {
                                            if (loadingFile
                                                    .containsKey(photoFileName)) {
                                                loadingFile
                                                        .remove(photoFileName);
                                                FileLoader
                                                        .getInstance()
                                                        .cancelLoadingForImageView(
                                                                photoImage);
                                                updateVisibleRows();
                                            } else {
                                                addToLoadingFile(photoFileName,
                                                        actionProgress);
                                                if (message.imagePreview != null) {
                                                    photoImage
                                                            .setImage(
                                                                    photoObjectToSet.photoOwner.location,
                                                                    photoFilter,
                                                                    message.imagePreview,
                                                                    photoObjectToSet.photoOwner.size);
                                                } else {
                                                    photoImage
                                                            .setImage(
                                                                    photoObjectToSet.photoOwner.location,
                                                                    photoFilter,
                                                                    message.messageOwner.out ? R.drawable.photo_placeholder_out
                                                                            : R.drawable.photo_placeholder_in,
                                                                    photoObjectToSet.photoOwner.size);
                                                }
                                                updateVisibleRows();
                                            }
                                        }
                                    }
                                }
                            }
                        });
            }

            if (photoImage != null) {
                photoImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        processOnClick(view);
                    }
                });

                photoImage
                        .setOnLongClickListener(new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View v) {
                                createMenu(v, false);
                                return true;
                            }
                        });
            }
        }

        private void alertUserOpenError() {
            AlertDialog.Builder builder = new AlertDialog.Builder(
                    parentActivity);
            builder.setTitle(R.string.AppName);
            builder.setPositiveButton(R.string.OK, null);
            if (message.type == 6 || message.type == 7) {
                builder.setMessage(R.string.NoPlayerInstalled);
            } else {
                builder.setMessage(LocaleController.formatString(
                        "NoHandleAppInstalled", R.string.NoHandleAppInstalled,
                        message.messageOwner.media.document.mime_type));
            }
            visibleDialog = builder.show();
            visibleDialog.setCanceledOnTouchOutside(true);

            visibleDialog
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            visibleDialog = null;
                        }
                    });
        }

        private void processOnClick(View view) {
            if (mActionMode != null) {
                processRowSelect(view);
                return;
            }
            if (message != null) {
                if (message.type == 4 || message.type == 5) {

                    //					if (LocaleController.getInstance().IsChineseMainLand()) // jenf
                    //																			// for
                    //																			// location
                    //					{
                    NotificationCenter.getInstance().addToMemCache(0,
                            message);
                    AmapLocationActivity fragment = new AmapLocationActivity();
                    ((LaunchActivity) parentActivity).presentFragment(
                            fragment, "location_view", false);
                    //					} else {
                    //						if (!isGoogleMapsInstalled()) {
                    //							return;
                    //						}
                    //						NotificationCenter.getInstance().addToMemCache(0,
                    //								message);
                    //						LocationActivity fragment = new LocationActivity();
                    //						((LaunchActivity) parentActivity).presentFragment(
                    //								fragment, "location_view", false);
                    //					}
                } else if (message.type == 2 || message.type == 3) {
                    if (photoFile == null || photoObjectToSet == null
                            || photoFile != null && photoFile.exists()) {
                        NotificationCenter.getInstance().addToMemCache(51,
                                message);
                        Intent intent = new Intent(parentActivity,
                                GalleryImageViewer.class);
                        startActivity(intent);
                    } else {
                        addToLoadingFile(photoFileName, actionProgress);
                        if (message.imagePreview != null) {
                            photoImage.setImage(
                                    photoObjectToSet.photoOwner.location,
                                    photoFilter, message.imagePreview,
                                    photoObjectToSet.photoOwner.size);
                        } else {
                            photoImage
                                    .setImage(
                                            photoObjectToSet.photoOwner.location,
                                            photoFilter,
                                            message.messageOwner.out ? R.drawable.photo_placeholder_out
                                                    : R.drawable.photo_placeholder_in,
                                            photoObjectToSet.photoOwner.size);
                        }
                        updateVisibleRows();
                    }
                } else if (message.type == 11) {
                    NotificationCenter.getInstance().addToMemCache(51, message);
                    Intent intent = new Intent(parentActivity,
                            GalleryImageViewer.class);
                    startActivity(intent);
                } else if (message.type == 6 || message.type == 7
                        || message.type == 16 || message.type == 17) {
                    File f = null;
                    String fileName = message.getFileName();
                    if (message.messageOwner.attachPath != null
                            && message.messageOwner.attachPath.length() != 0) {
                        f = new File(message.messageOwner.attachPath);
                    }
                    if (f == null || f != null && !f.exists()) {
                        f = new File(Utilities.getCacheDir(), fileName);
                    }
                    if (f != null && f.exists()) {
                        String realMimeType = null;
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            if (message.type == 6 || message.type == 7) {
                                intent.setDataAndType(Uri.fromFile(f),
                                        "video/mp4");
                            } else if (message.type == 16 || message.type == 17) {
                                MimeTypeMap myMime = MimeTypeMap.getSingleton();
                                int idx = fileName.lastIndexOf(".");
                                if (idx != -1) {
                                    String ext = fileName.substring(idx + 1);
                                    realMimeType = myMime
                                            .getMimeTypeFromExtension(ext
                                                    .toLowerCase());
                                    if (realMimeType != null) {
                                        intent.setDataAndType(Uri.fromFile(f),
                                                realMimeType);
                                    } else {
                                        intent.setDataAndType(Uri.fromFile(f),
                                                "text/plain");
                                    }
                                } else {
                                    intent.setDataAndType(Uri.fromFile(f),
                                            "text/plain");
                                }
                            }
                            if (realMimeType != null) {
                                try {
                                    startActivity(intent);
                                } catch (Exception e) {
                                    intent.setDataAndType(Uri.fromFile(f),
                                            "text/plain");
                                    startActivity(intent);
                                }
                            } else {
                                startActivity(intent);
                            }
                        } catch (Exception e) {
                            alertUserOpenError();
                        }
                    } else {
                        if (message.messageOwner.send_state != MessagesController.MESSAGE_SEND_STATE_SEND_ERROR
                                && message.messageOwner.send_state != MessagesController.MESSAGE_SEND_STATE_SENDING
                                || !message.messageOwner.out) {
                            if (!loadingFile.containsKey(fileName)) {
                                progressByTag.put(
                                        (Integer) actionProgress.getTag(),
                                        fileName);
                                addToLoadingFile(fileName, actionProgress);
                                if (message.type == 6 || message.type == 7) {
                                    FileLoader.getInstance().loadFile(
                                            message.messageOwner.media.video,
                                            null, null, null);
                                } else if (message.type == 16
                                        || message.type == 17) {
                                    FileLoader
                                            .getInstance()
                                            .loadFile(
                                                    null,
                                                    null,
                                                    message.messageOwner.media.document,
                                                    null);
                                }
                                updateVisibleRows();
                            }
                        } else {
                            if (message.messageOwner.send_state == MessagesController.MESSAGE_SEND_STATE_SEND_ERROR) {
                                createMenu(view, false);
                            }
                        }
                    }
                }
            }
        }
    }

    void setScreenBacklight(float a) {
        if (this.getActivity() == null)
            return;
        WindowManager.LayoutParams lp = this.getActivity().getWindow()
                .getAttributes();
        lp.screenBrightness = a;
        this.getActivity().getWindow().setAttributes(lp);
    }

    private void simulateProximitySensorNearby(boolean nearby) {
        if (this.getActivity() == null)
            return;
        final Window window = this.getActivity().getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        View view = ((ViewGroup) window.getDecorView().findViewById(
                android.R.id.content)).getChildAt(0);
        if (nearby) {
            params.screenBrightness = 0.1f;
            view.setVisibility(View.INVISIBLE);
        } else {
            params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
            view.setVisibility(View.VISIBLE);
        }
        window.setAttributes(params);
    }

    /**
     * Listener to changes in the proximity sensor state.
     */
    private class ProximitySensorListener implements
            ProximitySensorManager.Listener {
        /**
         * Used to show a blank view and hide the action bar.
         */
        private final Runnable mBlankRunnable = new Runnable() {
            @Override
            public void run() {
                // View blankView =
                // fragmentView.findViewById(R.id.progressLayout);
                // blankView.setVisibility(View.VISIBLE);
                // InMeetingActivity.this.getActivity().getActionBar().hide();
                FileLog.d("emm", "chat mBlankRunnable");
                // if(wakeLock != null)
                // {
                // if(!wakeLock.isHeld()) {
                // wakeLock.acquire();
                // }
                // }
                // else
                {
                    simulateProximitySensorNearby(true);
                }

                MediaController.getInstance().SetPlayoutSpeaker(false);
                if (!isNear)
                    MediaController.getInstance().seekCurrentToProgress(0, true);
                isNear = true;
            }
        };

        /**
         * Used to remove the blank view and show the action bar.
         */
        private final Runnable mUnblankRunnable = new Runnable() {
            @Override
            public void run() {
                FileLog.d("emm", "chat mUnblankRunnable");

                // View blankView =
                // fragmentView.findViewById(R.id.progressLayout);
                // blankView.setVisibility(View.GONE);
                // InMeetingActivity.this.getActivity().getActionBar().show();
                // if(wakeLock != null)
                // {
                // if(wakeLock.isHeld()) {
                // wakeLock.release();
                // }
                // }
                // else
                {
                    simulateProximitySensorNearby(false);
                }
                if (!WeiyiMeeting.isInMeeting())
                    MediaController.getInstance().SetPlayoutSpeaker(!MessagesController.getInstance().earphone);
                isNear = false;
            }
        };

        @Override
        public synchronized void onNear() {
            if (MediaController.getInstance().isAudioPaused())
                return;
            FileLog.d("emm", "chat onNear");
            clearPendingRequests();
            postDelayed(mBlankRunnable, PROXIMITY_BLANK_DELAY_MILLIS);
        }

        @Override
        public synchronized void onFar() {
            if (MediaController.getInstance().isAudioPaused())
                return;
            FileLog.d("emm", "chat onFar");
            clearPendingRequests();
            postDelayed(mUnblankRunnable, PROXIMITY_UNBLANK_DELAY_MILLIS);
        }

        /**
         * Removed any delayed requests that may be pending.
         */
        public synchronized void clearPendingRequests() {
            if (ChatActivity.this.fragmentView != null) {
                ChatActivity.this.fragmentView.removeCallbacks(mBlankRunnable);
                ChatActivity.this.fragmentView
                        .removeCallbacks(mUnblankRunnable);
            }
        }

        /**
         * Post a {@link Runnable} with a delay on the main thread.
         */
        private synchronized void postDelayed(Runnable runnable,
                                              long delayMillis) {
            // Post these instead of executing immediately so that:
            // - They are guaranteed to be executed on the main thread.
            // - If the sensor values changes rapidly for some time, the UI will
            // not be
            // updated immediately.
            if (ChatActivity.this.fragmentView != null)
                ChatActivity.this.fragmentView.postDelayed(runnable,
                        delayMillis);
        }
    }
}
