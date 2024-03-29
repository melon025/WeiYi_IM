/*
 * This is the source code of Emm for Android v. 1.3.2.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013.
 */

package info.emm.messenger;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import info.emm.LocalData.Config;
import info.emm.objects.MessageObject;
import info.emm.ui.ApplicationLoader;
import info.emm.ui.Views.BackupImageView;
import info.emm.ui.Views.ImageReceiver;
import info.emm.utils.Utilities;
import info.emm.yuanchengcloudb.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

public class FileLoader {
    public LruCache memCache;

    private String ignoreRemoval = null;
    private ConcurrentHashMap<String, CacheImage> imageLoading;
    private HashMap<Integer, CacheImage> imageLoadingByKeys;
    private Queue<FileLoadOperation> operationsQueue;
    private Queue<FileLoadOperation> runningOperation;
    private final int maxConcurentLoadingOpertaionsCount = 2;
    private Queue<FileUploadOperation> uploadOperationQueue;
    private ConcurrentHashMap<String, FileUploadOperation> uploadOperationPaths;
    private ConcurrentHashMap<String, FileUploadOperation> uploadOperationPathsEnc;
    private int currentUploadOperationsCount = 0;
    private Queue<FileLoadOperation> loadOperationQueue;
    private Queue<FileLoadOperation> audioLoadOperationQueue;
    private ConcurrentHashMap<String, FileLoadOperation> loadOperationPaths;
    private int currentLoadOperationsCount = 0;
    private int currentAudioLoadOperationsCount = 0;
    public static long lastCacheOutTime = 0;
    public ConcurrentHashMap<String, Float> fileProgresses = new ConcurrentHashMap<String, Float>();
    private long lastProgressUpdateTime = 0;
    private HashMap<String, Integer> BitmapUseCounts = new HashMap<String, Integer>();

    int lastImageNum;
    //xueqiang add for group image begin 
    public int iscrop=0;//0����Ҫ�ü�,1��Ҫ�ü�ͼ��
    public int groupid=0;    
    public int userid=0;
    public int companyid=0;
    public int randomfile=1;
    //xueqiang add for group image end

    public static final int FileDidUpload = 10000;
    public static final int FileDidFailUpload = 10001;
    public static final int FileUploadProgressChanged = 10002;
    public static final int FileLoadProgressChanged = 10003;
    public static final int FileDidLoaded = 10004;
    public static final int FileDidFailedLoad = 10005;
    
   

    public class VMRuntimeHack {
        private Object runtime = null;
        private Method trackAllocation = null;
        private Method trackFree = null;

        public boolean trackAlloc(long size) {
            if (runtime == null)
                return false;
            try {
                Object res = trackAllocation.invoke(runtime, size);
                return (res instanceof Boolean) ? (Boolean)res : true;
            } catch (IllegalArgumentException e) {
                return false;
            } catch (IllegalAccessException e) {
                return false;
            } catch (InvocationTargetException e) {
                return false;
            }
        }

        public boolean trackFree(long size) {
            if (runtime == null)
                return false;
            try {
                Object res = trackFree.invoke(runtime, size);
                return (res instanceof Boolean) ? (Boolean)res : true;
            } catch (IllegalArgumentException e) {
                return false;
            } catch (IllegalAccessException e) {
                return false;
            } catch (InvocationTargetException e) {
                return false;
            }
        }

        @SuppressWarnings("unchecked")
        public VMRuntimeHack() {
            boolean success = false;
            try {
                Class cl = Class.forName("dalvik.system.VMRuntime");
                Method getRt = cl.getMethod("getRuntime", new Class[0]);
                runtime = getRt.invoke(null, new Object[0]);
                trackAllocation = cl.getMethod("trackExternalAllocation", new Class[] {long.class});
                trackFree = cl.getMethod("trackExternalFree", new Class[] {long.class});
                success = true;
            } catch (ClassNotFoundException e) {
                FileLog.e("emm", e);
            } catch (SecurityException e) {
                FileLog.e("emm", e);
            } catch (NoSuchMethodException e) {
                FileLog.e("emm", e);
            } catch (IllegalArgumentException e) {
                FileLog.e("emm", e);
            } catch (IllegalAccessException e) {
                FileLog.e("emm", e);
            } catch (InvocationTargetException e) {
                FileLog.e("emm", e);
            }
            if (!success) {
                runtime = null;
                trackAllocation = null;
                trackFree = null;
            }
        }
    }

    public VMRuntimeHack runtimeHack = null;

    private class CacheImage {
        public String key;
        final public ArrayList<Object> imageViewArray = new ArrayList<Object>();
        public FileLoadOperation loadOperation;

        public void addImageView(Object imageView) {
            synchronized (imageViewArray) {
                boolean exist = false;
                for (Object v : imageViewArray) {
                    if (v == imageView) {
                        exist = true;
                        break;
                    }
                }
                if (!exist) {
                    imageViewArray.add(imageView);
                }
            }
        }

        public void removeImageView(Object imageView) {
            synchronized (imageViewArray) {
                for (int a = 0; a < imageViewArray.size(); a++) {
                    Object obj = imageViewArray.get(a);
                    if (obj == null || obj == imageView) {
                        imageViewArray.remove(a);
                        a--;
                    }
                }
            }
        }

        public void callAndClear(Bitmap image) {
            synchronized (imageViewArray) {
                if (image != null) {
                    for (Object imgView : imageViewArray) {
                        if (imgView instanceof BackupImageView) {
                            ((BackupImageView)imgView).setImageBitmap(image, key);
                        } else if (imgView instanceof ImageReceiver) {
                            ((ImageReceiver)imgView).setImageBitmap(image, key);
                        }
                    }
                }
            }
            Utilities.imageLoadQueue.postRunnable(new Runnable() {
                @Override
                public void run() {
                    synchronized (imageViewArray) {
                        imageViewArray.clear();
                    }
                    loadOperation = null;
                }
            });
        }

        public void cancelAndClear() {
            if (loadOperation != null) {
                loadOperation.cancel();
                loadOperation = null;
            }
            synchronized (imageViewArray) {
                imageViewArray.clear();
            }
        }
    }

    public void incrementUseCount(String key) {
        Integer count = BitmapUseCounts.get(key);
        if (count == null) {
            BitmapUseCounts.put(key, 1);
        } else {
            BitmapUseCounts.put(key, count + 1);
        }
    }

    public boolean decrementUseCount(String key) {
        Integer count = BitmapUseCounts.get(key);
        if (count == null) {
            return true;
        }
        if (count == 1) {
            BitmapUseCounts.remove(key);
            return true;
        } else {
            BitmapUseCounts.put(key, count - 1);
        }
        return false;
    }

    public void removeImage(String key) {
        BitmapUseCounts.remove(key);
        try {
        	memCache.remove(key);
        } catch (Exception e) {
           // FileLog.e("emm", e);
        }        
    }

    /*class BitmapWorkerTask extends AsyncTask<Integer, Void, Bitmap> {
        private CacheImage cacheImage;
        private Bitmap bitmap;
        private int data = 0;

        public BitmapWorkerTask(ArrayList<WeakReference<View>> arr) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = new WeakReference<ImageView>(imageView);
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(Integer... params) {
            data = params[0];
            return decodeSampledBitmapFromResource(getResources(), data, 100, 100));
        }

        // Once complete, see if ImageView is still around and set bitmap.
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (imageViewReference != null && bitmap != null) {
                final ImageView imageView = imageViewReference.get();
                if (imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
    }*/

    private static volatile FileLoader Instance = null;
    public static FileLoader getInstance() {
        FileLoader localInstance = Instance;
        if (localInstance == null) {
            synchronized (FileLoader.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new FileLoader();
                }
            }
        }
        return localInstance;
    }

    public FileLoader() {
        int cacheSize = Math.min(15, ((ActivityManager) ApplicationLoader.applicationContext.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass() / 7) * 1024 * 1024;

        if (Build.VERSION.SDK_INT < 11) {
            runtimeHack = new VMRuntimeHack();
            cacheSize = 1024 * 1024 * 3;
        }
        memCache = new LruCache(cacheSize) {
            @SuppressLint("NewApi")
			@Override
            protected int sizeOf(String key, Bitmap bitmap) {
                if(Build.VERSION.SDK_INT < 12) {
                    return bitmap.getRowBytes() * bitmap.getHeight();
                } else {
                    return bitmap.getByteCount();
                }
            }
            @Override
            protected void entryRemoved(boolean evicted, String key, Bitmap oldBitmap, Bitmap newBitmap) {
                if (ignoreRemoval != null && key != null && ignoreRemoval.equals(key)) {
                    return;
                }
                Integer count = BitmapUseCounts.get(key);
                if (count == null || count == 0) {
                    if (runtimeHack != null) {
                        runtimeHack.trackAlloc(oldBitmap.getRowBytes() * oldBitmap.getHeight());
                    }
                    if (Build.VERSION.SDK_INT < 11) {
                        if (!oldBitmap.isRecycled()) {
                            oldBitmap.recycle();
                        }
                    }
                }
            }
        };
        imageLoading = new ConcurrentHashMap<String, CacheImage>();
        imageLoadingByKeys = new HashMap<Integer, CacheImage>();
        operationsQueue = new LinkedList<FileLoadOperation>();
        runningOperation = new LinkedList<FileLoadOperation>();
        uploadOperationQueue = new LinkedList<FileUploadOperation>();
        uploadOperationPaths = new ConcurrentHashMap<String, FileUploadOperation>();
        uploadOperationPathsEnc = new ConcurrentHashMap<String, FileUploadOperation>();
        loadOperationPaths = new ConcurrentHashMap<String, FileLoadOperation>();
        loadOperationQueue = new LinkedList<FileLoadOperation>();
        audioLoadOperationQueue = new LinkedList<FileLoadOperation>();
    }

    public void cancelUploadFile(final String location, final boolean enc) {
        Utilities.fileUploadQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                if (!enc) {
                    FileUploadOperation operation = uploadOperationPaths.get(location);
                    if (operation != null) {
                        uploadOperationQueue.remove(operation);
                        operation.cancel();
                    }
                } else {
                    FileUploadOperation operation = uploadOperationPathsEnc.get(location);
                    if (operation != null) {
                        uploadOperationQueue.remove(operation);
                        operation.cancel();
                    }
                }
            }
        });
    }

    public boolean isInCache(String key) {
        return memCache.get(key) != null;
    }

    public void uploadFile(final String location, final byte[] key, final byte[] iv) {
//    	FileLog.d("emm", "uploadFile:" + location);
        Utilities.fileUploadQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                if (key != null) {
                    if (uploadOperationPathsEnc.containsKey(location)) {
                        return;
                    }
                } else {
                    if (uploadOperationPaths.containsKey(location)) {
                        return;
                    }
                }
                
//                FileLog.d("emm", "uploadFile acture:" + location);
                //sam todo..��Ҫ��IP�仯Ϊһ������
                //FileUploadOperation operation = new FileUploadOperation(location, key, iv);
                //���һ��������ʾ�Լ�������ļ�����������ͷ���ʱ����Ҫָ����Ŀǰ������ͷ����һ����ͼһ��Сͼ��Ӧ����PHP��һ��ת��
                
                FileUploadOperation operation = new FileUploadOperation(location, Config.webfun_uploadfile, null);
                operation.groupid = groupid;
                operation.iscrop = iscrop;
                operation.userid = userid;
                operation.companyid = companyid;
                operation.randomfile=randomfile;
                		
                
                groupid=0;
                userid=0;
                iscrop =0;
                randomfile=1;
                
                if (key != null) {
                    uploadOperationPathsEnc.put(location, operation);
                } else {
                    uploadOperationPaths.put(location, operation);
                }
                operation.delegate = new FileUploadOperation.FileUploadOperationDelegate() {
                    @Override
                    public void didFinishUploadingFile(FileUploadOperation operation, final TLRPC.InputFile inputFile, final TLRPC.InputEncryptedFile inputEncryptedFile, final String url) {
//                    	FileLog.d("emm", "didFinishUploadingFile acture:" + location);
                        Utilities.fileUploadQueue.postRunnable(new Runnable() {
                            @Override
                            public void run() {
//                            	FileLog.d("emm", "stageQueue 1 didFinishUploadingFile acture:" + location);
                                Utilities.stageQueue.postRunnable(new Runnable() {
                                    @Override
                                    public void run() {
//                                    	FileLog.d("emm", "stageQueue 2 didFinishUploadingFile acture:" + location);
                                    	//�˴�������url���������ͷ��͸���ͷ�������xueqiang
                                        NotificationCenter.getInstance().postNotificationName(FileDidUpload, location, inputFile, inputEncryptedFile, url);
                                        fileProgresses.remove(location);
                                    }
                                });
                                if (key != null) {
                                    uploadOperationPathsEnc.remove(location);
                                } else {
                                    uploadOperationPaths.remove(location);
                                }
                                currentUploadOperationsCount--;
                                if (currentUploadOperationsCount < 2) {
                                    FileUploadOperation operation = uploadOperationQueue.poll();
                                    if (operation != null) {
//                                    	FileLog.d("emm", "start new");
                                        currentUploadOperationsCount++;
                                        operation.start();
                                    }
                                }
                            }
                        });
                    }

                    @Override
                    public void didFailedUploadingFile(final FileUploadOperation operation) {
                        Utilities.fileUploadQueue.postRunnable(new Runnable() {
                            @Override
                            public void run() {
                                Utilities.stageQueue.postRunnable(new Runnable() {
                                    @Override
                                    public void run() {
                                        fileProgresses.remove(location);
                                        if (operation.state != 2) {
                                            NotificationCenter.getInstance().postNotificationName(FileDidFailUpload, location, key != null);
                                        }
                                    }
                                });
                                if (key != null) {
                                    uploadOperationPathsEnc.remove(location);
                                } else {
                                    uploadOperationPaths.remove(location);
                                }
                                currentUploadOperationsCount--;
                                if (currentUploadOperationsCount < 2) {
                                    FileUploadOperation operation = uploadOperationQueue.poll();
                                    if (operation != null) {
                                        currentUploadOperationsCount++;
                                        operation.start();
                                    }
                                }
                            }
                        });
                    }

                    @Override
                    public void didChangedUploadProgress(FileUploadOperation operation, final float progress) {
                        if (operation.state != 2) {
                            fileProgresses.put(location, progress);
                        }
                        long currentTime = System.currentTimeMillis();
                        if (lastProgressUpdateTime == 0 || lastProgressUpdateTime < currentTime - 500) {
                            lastProgressUpdateTime = currentTime;
                            Utilities.RunOnUIThread(new Runnable() {
                                @Override
                                public void run() {
                                    NotificationCenter.getInstance().postNotificationName(FileUploadProgressChanged, location, progress, key != null);
                                }
                            });
                        }
                    }
                };
                if (currentUploadOperationsCount < 2) {
                    currentUploadOperationsCount++;
                    operation.start();
                } else {
                    uploadOperationQueue.add(operation);
                }
            }
        });
    }

    public void cancelLoadFile(final TLRPC.Video video, final TLRPC.PhotoSize photo, final TLRPC.Document document, final TLRPC.Audio audio) {
        if (video == null && photo == null && document == null && audio == null) {
            return;
        }
        Utilities.fileUploadQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                String fileName = null;
                if (video != null) {
                    fileName = MessageObject.getAttachFileName(video);
                } else if (photo != null) {
                    fileName = MessageObject.getAttachFileName(photo);
                } else if (document != null) {
                    fileName = MessageObject.getAttachFileName(document);
                } else if (audio != null) {
                    fileName = MessageObject.getAttachFileName(audio);
                }
                if (fileName == null) {
                    return;
                }
                FileLoadOperation operation = loadOperationPaths.get(fileName);
                if (operation != null) {
                    if (audio != null) {
                        audioLoadOperationQueue.remove(operation);
                    } else {
                        loadOperationQueue.remove(operation);
                    }
                    operation.cancel();
                }
            }
        });
    }

    public boolean isLoadingFile(String fileName) {
        return loadOperationPaths.containsKey(fileName);
    }

    public void loadFile(final TLRPC.Video video, final TLRPC.PhotoSize photo, final TLRPC.Document document, final TLRPC.Audio audio) {
        Utilities.fileUploadQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                String fileName = null;
                if (video != null) {
                    fileName = MessageObject.getAttachFileName(video);
                } else if (photo != null) {
                    fileName = MessageObject.getAttachFileName(photo);
                } else if (document != null) {
                    fileName = MessageObject.getAttachFileName(document);
                } else if (audio != null) 
                {
                    fileName = MessageObject.getAttachFileName(audio);
                    //xueqiang change
                    final String f = fileName;
                    Utilities.RunOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            NotificationCenter.getInstance().postNotificationName(FileDidLoaded, f);
                        }
                    });
                    return;
                }
                if (fileName == null || fileName.contains("" + Integer.MIN_VALUE)) {
                    return;
                }
                if (loadOperationPaths.containsKey(fileName)) {
                    return;
                }
                FileLoadOperation operation = null;
                if (video != null) {
                    operation = new FileLoadOperation(video);
                    operation.totalBytesCount = video.size;
                } else if (photo != null) {
                    operation = new FileLoadOperation(photo.location);
                    operation.totalBytesCount = photo.size;
                    operation.needBitmapCreate = false;
                } else if (document != null) {
                    operation = new FileLoadOperation(document);
                    operation.totalBytesCount = document.size;
                } else if (audio != null) {
                    operation = new FileLoadOperation(audio);
                    operation.totalBytesCount = audio.size;
                }

                final String arg1 = fileName;
                loadOperationPaths.put(fileName, operation);
                operation.delegate = new FileLoadOperation.FileLoadOperationDelegate() {
                    @Override
                    public void didFinishLoadingFile(FileLoadOperation operation) {
                        Utilities.RunOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                                NotificationCenter.getInstance().postNotificationName(FileLoadProgressChanged, arg1, 1.0f);
                            }
                        });
                        Utilities.RunOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                                NotificationCenter.getInstance().postNotificationName(FileDidLoaded, arg1);
                            }
                        });
                        Utilities.fileUploadQueue.postRunnable(new Runnable() {
                            @Override
                            public void run() {
                                loadOperationPaths.remove(arg1);
                                if (audio != null) {
                                    currentAudioLoadOperationsCount--;
                                    if (currentAudioLoadOperationsCount < 2) {
                                        FileLoadOperation operation = audioLoadOperationQueue.poll();
                                        if (operation != null) {
                                            currentAudioLoadOperationsCount++;
                                            operation.start();
                                        }
                                    }
                                } else {
                                    currentLoadOperationsCount--;
                                    if (currentLoadOperationsCount < 2) {
                                        FileLoadOperation operation = loadOperationQueue.poll();
                                        if (operation != null) {
                                            currentLoadOperationsCount++;
                                            operation.start();
                                        }
                                    }
                                }
                            }
                        });
                        fileProgresses.remove(arg1);
                    }

                    @Override
                    public void didFailedLoadingFile(FileLoadOperation operation) {
                        fileProgresses.remove(arg1);
                        if (operation.state != 2) {
                            Utilities.RunOnUIThread(new Runnable() {
                                @Override
                                public void run() {
                                    NotificationCenter.getInstance().postNotificationName(FileDidFailedLoad, arg1);
                                }
                            });
                        }
                        Utilities.fileUploadQueue.postRunnable(new Runnable() {
                            @Override
                            public void run() {
                                loadOperationPaths.remove(arg1);
                                if (audio != null) {
                                    currentAudioLoadOperationsCount--;
                                    if (currentAudioLoadOperationsCount < 2) {
                                        FileLoadOperation operation = audioLoadOperationQueue.poll();
                                        if (operation != null) {
                                            currentAudioLoadOperationsCount++;
                                            operation.start();
                                        }
                                    }
                                } else {
                                    currentLoadOperationsCount--;
                                    if (currentLoadOperationsCount < 2) {
                                        FileLoadOperation operation = loadOperationQueue.poll();
                                        if (operation != null) {
                                            currentLoadOperationsCount++;
                                            operation.start();
                                        }
                                    }
                                }
                            }
                        });
                    }

                    @Override
                    public void didChangedLoadProgress(FileLoadOperation operation, final float progress) {
                        if (operation.state != 2) {
                            fileProgresses.put(arg1, progress);
                        }
                        long currentTime = System.currentTimeMillis();
                        if (lastProgressUpdateTime == 0 || lastProgressUpdateTime < currentTime - 500) {
                            lastProgressUpdateTime = currentTime;
                            Utilities.RunOnUIThread(new Runnable() {
                                @Override
                                public void run() {
                                    NotificationCenter.getInstance().postNotificationName(FileLoadProgressChanged, arg1, progress);
                                }
                            });
                        }
                    }
                };
                if (audio != null) {
                    if (currentAudioLoadOperationsCount < 2) {
                        currentAudioLoadOperationsCount++;
                        operation.start();
                    } else {
                        audioLoadOperationQueue.add(operation);
                    }
                } else {
                    if (currentLoadOperationsCount < 2) {
                        currentLoadOperationsCount++;
                        operation.start();
                    } else {
                        loadOperationQueue.add(operation);
                    }
                }
            }
        });
    }

    Bitmap imageFromKey(String key) {
        if (key == null) {
            return null;
        }
        return memCache.get(key);
    }

    public void clearMemory() {
        memCache.evictAll();
    }

    private Integer getTag(Object obj) {
        if (obj instanceof BackupImageView) {
            return (Integer)((BackupImageView)obj).getTag(R.string.CacheTag);
        } else if (obj instanceof ImageReceiver) {
            return ((ImageReceiver)obj).TAG;
        }
        return 0;
    }

    private void setTag(Object obj, Integer tag) {
        if (obj instanceof BackupImageView) {
            ((BackupImageView)obj).setTag(R.string.CacheTag, tag);
        } else if (obj instanceof ImageReceiver) {
            ((ImageReceiver)obj).TAG = tag;
        }
    }

    public void cancelLoadingForImageView(final Object imageView) {
        if (imageView == null) {
            return;
        }
        Utilities.imageLoadQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                Integer TAG = getTag(imageView);
                if (TAG == null) {
                    TAG = lastImageNum;
                    setTag(imageView, TAG);
                    lastImageNum++;
                    if (lastImageNum == Integer.MAX_VALUE) {
                        lastImageNum = 0;
                    }
                }
                CacheImage ei = imageLoadingByKeys.get(TAG);
                if (ei != null) {
                    imageLoadingByKeys.remove(TAG);
                    ei.removeImageView(imageView);
                    if (ei.imageViewArray.size() == 0) {
                        checkOperationsAndClear(ei.loadOperation);
                        ei.cancelAndClear();
                        imageLoading.remove(ei.key);
                    }
                }
            }
        });
    }

    public Bitmap getImageFromMemory(TLRPC.FileLocation url, Object imageView, String filter, boolean cancel) {
        return getImageFromMemory(url, null, imageView, filter, cancel);
    }

    public Bitmap getImageFromMemory(String url, Object imageView, String filter, boolean cancel) {
        return getImageFromMemory(null, url, imageView, filter, cancel);
    }

    public Bitmap getImageFromMemory(TLRPC.FileLocation url, String httpUrl, Object imageView, String filter, boolean cancel) {
        if (url == null && httpUrl == null) {
            return null;
        }
        String key;
        if (httpUrl != null) {
            key = Utilities.MD5(httpUrl);
        } else {
            key = Utilities.MD5(url.http_path_img);//url.volume_id + "_" + url.local_id;
        }
        if (filter != null) {
            key += "@" + filter;
        }

        Bitmap img = imageFromKey(key);
        if (imageView != null && img != null && cancel) {
            cancelLoadingForImageView(imageView);
        }
        return img;
    }

    private void performReplace(String oldKey, String newKey) {
        Bitmap b = memCache.get(oldKey);
        if (b != null) {
            ignoreRemoval = oldKey;
            memCache.remove(oldKey);
            memCache.put(newKey, b);
            ignoreRemoval = null;
        }
        Integer val = BitmapUseCounts.get(oldKey);
        if (val != null) {
            BitmapUseCounts.put(newKey, val);
            BitmapUseCounts.remove(oldKey);
        }
    }

    public void replaceImageInCache(final String oldKey, final String newKey) {
        Utilities.RunOnUIThread(new Runnable() {
            @Override
            public void run() {
                ArrayList<String> arr = memCache.getFilterKeys(oldKey);
                if (arr != null) {
                    for (String filter : arr) {
                        performReplace(oldKey + "@" + filter, newKey + "@" + filter);
                    }
                } else {
                    performReplace(oldKey, newKey);
                }
            }
        });
    }

    public void loadImage(final String url, final Object imageView, final String filter, final boolean cancel) {
        loadImage(null, url, imageView, filter, cancel, 0);
    }

    public void loadImage(final TLRPC.FileLocation url, final Object imageView, final String filter, final boolean cancel) {
        loadImage(url, null, imageView, filter, cancel, 0);
    }

    public void loadImage(final TLRPC.FileLocation url, final Object imageView, final String filter, final boolean cancel, final int size) {
        loadImage(url, null, imageView, filter, cancel, size);
    }

    public void loadImage(final TLRPC.FileLocation url, final String httpUrl, final Object imageView, final String filter, final boolean cancel, final int size) {
        if ((url == null && httpUrl == null) || imageView == null || (url != null && !(url instanceof TLRPC.TL_fileLocation) && !(url instanceof TLRPC.TL_fileEncryptedLocation))) {
            return;
        }
//        if(url.http_path_img.contains("http://api.yunboxin.com_small"))
//        {
//        	int i=0;
//        	i=2;
//        }
        Utilities.imageLoadQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                String key;
                if (httpUrl != null) {
                    key = Utilities.MD5(httpUrl);
                } else {
                    //key = url.volume_id + "_" + url.local_id;
                	key = Utilities.MD5(url.http_path_img);
                }
                if (filter != null) {
                    key += "@" + filter;
                }

                Integer TAG = getTag(imageView);
                if (TAG == null) {
                    TAG = lastImageNum;
                    setTag(imageView, TAG);
                    lastImageNum++;
                    if (lastImageNum == Integer.MAX_VALUE)
                        lastImageNum = 0;
                }

                boolean added = false;
                boolean addToByKeys = true;
                CacheImage alreadyLoadingImage = imageLoading.get(key);
                if (cancel) {
                    CacheImage ei = imageLoadingByKeys.get(TAG);
                    if (ei != null) {
                        if (ei != alreadyLoadingImage) {
                            ei.removeImageView(imageView);
                            if (ei.imageViewArray.size() == 0) {
                                checkOperationsAndClear(ei.loadOperation);
                                ei.cancelAndClear();
                                imageLoading.remove(ei.key);
                            }
                        } else {
                            addToByKeys = false;
                            added = true;
                        }
                    }
                }

                if (alreadyLoadingImage != null && addToByKeys) {
                    alreadyLoadingImage.addImageView(imageView);
                    imageLoadingByKeys.put(TAG, alreadyLoadingImage);
                    added = true;
                }

                if (!added) 
                {
                    final CacheImage img = new CacheImage();
                    img.key = key;
                    img.addImageView(imageView);
                    imageLoadingByKeys.put(TAG, img);
                    imageLoading.put(key, img);

                    final String arg2 = key;
                    FileLoadOperation loadOperation;
                    if (httpUrl != null)
                    {
                    	loadOperation = new FileLoadOperation(httpUrl);
                    } 
                    else 
                    {	
                        loadOperation = new FileLoadOperation(url);
                    }
                    loadOperation.totalBytesCount = size;
                    loadOperation.filter = filter;
                    loadOperation.delegate = new FileLoadOperation.FileLoadOperationDelegate() {
                        @Override
                        public void didFinishLoadingFile(final FileLoadOperation operation) 
                        {
                        	//�ӱ���Ӳ�̼����ļ�������һ��ͼƬ����operation.image,xueqiangע��
                            enqueueImageProcessingOperationWithImage(operation.image, filter, arg2, img);
                            if (operation.totalBytesCount != 0) {
                                final String arg1 = operation.location.volume_id + "_" + operation.location.local_id + ".jpg";
                                fileProgresses.remove(arg1);
                                Utilities.RunOnUIThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        NotificationCenter.getInstance().postNotificationName(FileLoadProgressChanged, arg1, 1.0f);
                                    }
                                });
                                Utilities.RunOnUIThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        NotificationCenter.getInstance().postNotificationName(FileDidLoaded, arg1);
                                    }
                                });
                            }
                        }

                        @Override
                        public void didFailedLoadingFile(final FileLoadOperation operation) {
                            Utilities.imageLoadQueue.postRunnable(new Runnable() {
                                @Override
                                public void run() {
                                    for (Object view : img.imageViewArray) {
                                        imageLoadingByKeys.remove(getTag(view));
                                        imageLoading.remove(arg2);
                                        checkOperationsAndClear(operation);
                                    }
                                }
                            });
                            Utilities.RunOnUIThread(new Runnable() {
                                @Override
                                public void run() {
                                    img.callAndClear(null);
                                }
                            });
                            if (operation.totalBytesCount != 0) {
                                final String arg1 = operation.location.volume_id + "_" + operation.location.local_id + ".jpg";
                                fileProgresses.remove(arg1);
                                if (operation.state != 2) {
                                    Utilities.RunOnUIThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            NotificationCenter.getInstance().postNotificationName(FileDidFailedLoad, arg1);
                                        }
                                    });
                                }
                            }
                        }

                        @Override
                        public void didChangedLoadProgress(FileLoadOperation operation, final float progress) {
                            if (operation.totalBytesCount != 0) {
                                final String arg1 = operation.location.volume_id + "_" + operation.location.local_id + ".jpg";
                                if (operation.state != 2) {
                                    fileProgresses.put(arg1, progress);
                                }
                                long currentTime = System.currentTimeMillis();
                                if (lastProgressUpdateTime == 0 || lastProgressUpdateTime < currentTime - 50) {
                                    lastProgressUpdateTime = currentTime;
                                    Utilities.RunOnUIThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            NotificationCenter.getInstance().postNotificationName(FileLoadProgressChanged, arg1, progress);
                                        }
                                    });
                                }
                            }
                        }
                    };
                    img.loadOperation = loadOperation;
                    if (runningOperation.size() < maxConcurentLoadingOpertaionsCount) {
                        loadOperation.start();
                        runningOperation.add(loadOperation);
                    } else {
                        operationsQueue.add(loadOperation);
                    }
                }
            }
        });
    }

    private void checkOperationsAndClear(FileLoadOperation operation) {
        operationsQueue.remove(operation);
        runningOperation.remove(operation);
        while (runningOperation.size() < maxConcurentLoadingOpertaionsCount && operationsQueue.size() != 0) {
            FileLoadOperation loadOperation = operationsQueue.poll();
            runningOperation.add(loadOperation);
            loadOperation.start();
        }
    }

    public void processImage(Bitmap image, Object imageView, String filter, boolean cancel) {
        if (filter == null || imageView == null) {
            return;
        }


        Integer TAG = getTag(imageView);
        if (TAG == null) {
            TAG = lastImageNum;
            setTag(image, TAG);
            lastImageNum++;
            if (lastImageNum == Integer.MAX_VALUE)
                lastImageNum = 0;
        }

        boolean added = false;
        boolean addToByKeys = true;
        CacheImage alreadyLoadingImage = imageLoading.get(filter);
        if (cancel) {
            CacheImage ei = imageLoadingByKeys.get(TAG);
            if (ei != null) {
                if (ei != alreadyLoadingImage) {
                    ei.removeImageView(imageView);
                    if (ei.imageViewArray.size() == 0) {
                        checkOperationsAndClear(ei.loadOperation);
                        ei.cancelAndClear();
                        imageLoading.remove(ei.key);
                    }
                } else {
                    addToByKeys = false;
                    added = true;
                }
            }
        }

        if (alreadyLoadingImage != null && addToByKeys) {
            alreadyLoadingImage.addImageView(imageView);
            imageLoadingByKeys.put(TAG, alreadyLoadingImage);
            added = true;
        }

        if (!added) {
            CacheImage img = new CacheImage();
            img.key = filter;
            img.addImageView(imageView);
            imageLoadingByKeys.put(TAG, img);
            imageLoading.put(filter, img);

            enqueueImageProcessingOperationWithImage(image, filter, filter, img);
        }
    }

    void enqueueImageProcessingOperationWithImage(final Bitmap image, final String filter, final String key, final CacheImage img) {
        if (key == null) {
            return;
        }

        Utilities.imageLoadQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                for (Object v : img.imageViewArray) {
                    imageLoadingByKeys.remove(getTag(v));
                }
                checkOperationsAndClear(img.loadOperation);
                imageLoading.remove(key);
            }
        });

        Utilities.RunOnUIThread(new Runnable() {
            @Override
            public void run() {
                img.callAndClear(image);
                if (image != null && memCache.get(key) == null) {
                    memCache.put(key, image);
                }
            }
        });
    }

    public static Bitmap loadBitmap(String path, Uri uri, float maxWidth, float maxHeight) {
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        FileDescriptor fileDescriptor = null;
        ParcelFileDescriptor parcelFD = null;

        if (path == null && uri != null && uri.getScheme() != null) {
            String imageFilePath = null;
            if (uri.getScheme().contains("file")) {
                path = uri.getPath();
            } else {
                try {
                    path = Utilities.getPath(uri);
                } catch (Exception e) {
                    FileLog.e("emm", e);
                }
            }
        }

        if (path != null) {
            BitmapFactory.decodeFile(path, bmOptions);
        } else if (uri != null) {
            boolean error = false;
            try {
                parcelFD = ApplicationLoader.applicationContext.getContentResolver().openFileDescriptor(uri, "r");
                fileDescriptor = parcelFD.getFileDescriptor();
                BitmapFactory.decodeFileDescriptor(fileDescriptor, null, bmOptions);
            } catch (Exception e) {
                FileLog.e("emm", e);
                try {
                    if (parcelFD != null) {
                        parcelFD.close();
                    }
                } catch (Exception e2) {
                    FileLog.e("emm", e2);
                }
                return null;
            }
        }
        float photoW = bmOptions.outWidth;
        float photoH = bmOptions.outHeight;
        float scaleFactor = Math.max(photoW / maxWidth, photoH / maxHeight);
        if (scaleFactor < 1) {
            scaleFactor = 1;
        }
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = (int)scaleFactor;

        String exifPath = null;
        if (path != null) {
            exifPath = path;
        } else if (uri != null) {
            exifPath = Utilities.getPath(uri);
        }

        Matrix matrix = null;

        if (exifPath != null) {
            ExifInterface exif;
            try {
                exif = new ExifInterface(exifPath);
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
                matrix = new Matrix();
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        matrix.postRotate(90);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        matrix.postRotate(180);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        matrix.postRotate(270);
                        break;
                }
            } catch (Exception e) {
                FileLog.e("emm", e);
            }
        }

        Bitmap b = null;
        if (path != null) {
            try {
                b = BitmapFactory.decodeFile(path, bmOptions);
                if (b != null) {
                    b = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, true);
                }
            } catch (Exception e) {
                FileLog.e("emm", e);
                FileLoader.getInstance().memCache.evictAll();
                if (b == null) {
                    b = BitmapFactory.decodeFile(path, bmOptions);
                }
                if (b != null) {
                    b = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, true);
                }
            }
        } else if (uri != null) {
            try {
                b = BitmapFactory.decodeFileDescriptor(fileDescriptor, null, bmOptions);
                if (b != null) {
                    b = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, true);
                }
            } catch (Exception e) {
                FileLog.e("emm", e);
            } finally {
                try {
                    if (parcelFD != null) {
                        parcelFD.close();
                    }
                } catch (Exception e) {
                    FileLog.e("emm", e);
                }
            }
        }

        return b;
    }

    public static TLRPC.PhotoSize scaleAndSaveImage(Bitmap bitmap, float maxWidth, float maxHeight, int quality, boolean cache) {
        if (bitmap == null) {
            return null;
        }
        float photoW = bitmap.getWidth();
        float photoH = bitmap.getHeight();
        if (photoW == 0 || photoH == 0) {
            return null;
        }
        float scaleFactor = Math.max(photoW / maxWidth, photoH / maxHeight);

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, (int)(photoW / scaleFactor), (int)(photoH / scaleFactor), true);

        TLRPC.TL_fileLocation location = new TLRPC.TL_fileLocation();
        location.volume_id = UserConfig.getSeq();	
        location.dc_id = Integer.MIN_VALUE;
        location.local_id = UserConfig.clientUserId;
        //sam
        location.http_path_img = "";
        //UserConfig.lastLocalId--;
        
        TLRPC.PhotoSize size;
        if (!cache) {
            size = new TLRPC.TL_photoSize();
        } else {
            size = new TLRPC.TL_photoCachedSize();
        }
        size.location = location;
        size.w = (int)(photoW / scaleFactor);
        size.h = (int)(photoH / scaleFactor);
        try {
            if (!cache) {
                String fileName = location.volume_id + "_" + location.local_id + ".jpg";
                final File cacheFile = new File(Utilities.getCacheDir(), fileName);
                FileOutputStream stream = new FileOutputStream(cacheFile);
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
                size.size = (int)stream.getChannel().size();
            } else {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
                size.bytes = stream.toByteArray();
                size.size = size.bytes.length;
            }
            if (Build.VERSION.SDK_INT < 11) {
                if (scaledBitmap != bitmap) {
                    scaledBitmap.recycle();
                }
            }
            return size;
        } catch (Exception e) {
            return null;
        }
    }
}
