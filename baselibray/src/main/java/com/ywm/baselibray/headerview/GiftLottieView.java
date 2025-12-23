//package com.ywm.baselibray.headerview;
//
//import com.dd.base.bean.gift.VideoGiftInfo;
//
//import android.content.Context;
//import android.text.TextUtils;
//import android.util.AttributeSet;
//import android.view.View;
//
//import com.airbnb.lottie.Cancellable;
//import com.airbnb.lottie.LottieComposition;
//import com.airbnb.lottie.OnCompositionLoadedListener;
//import com.immomo.momo.android.view.image.MomoLottieAnimationView;
//import com.immomo.momo.android.view.util.LottieUtils;
//
//public class GiftLottieView extends MomoLottieAnimationView {
//
//    private Cancellable cancellable;
//    private String fileName;
//    private static String folderName = "super_gift/imgs";
//    private static String advance = "super_gift/json/bottom_plate_l3.json";
//    private static String superAdvance = "super_gift/json/bottom_plate_l4.json";
//    private static String comboLevel2 = "super_gift/json/bottom_plate_effect_level2.json";
//
//    public GiftLottieView(Context context) {
//        this(context, null);
//    }
//
//    public GiftLottieView(Context context, AttributeSet attrs) {
//        this(context, attrs, 0);
//    }
//
//    public GiftLottieView(Context context, AttributeSet attrs, int defStyleAttr) {
//        super(context, attrs, defStyleAttr);
//        setFps(30);
//    }
//
//    private void init(String fileName) {
//        if (TextUtils.equals(fileName, this.fileName)) {
//            if (!isAnimating() && getVisibility() == View.VISIBLE) {
//                cancellable = LottieUtils.loadJsonAndPlay(fileName, this, false);
//            }
//            return;
//        }
//        resetAnim();
//        this.fileName = fileName;
//        setImageAssetsFolder(folderName);
//        cancellable = LottieComposition.Factory.fromAssetFileName(getContext(), fileName, new OnCompositionLoadedListener() {
//            @Override
//            public void onCompositionLoaded(LottieComposition composition) {
//                setComposition(composition);
//                playAnimation();
//            }
//        });
//    }
//
//    /**
//     * 设置礼物等级
//     * @param level
//     */
//    public void setGiftLevel(int level) {
//        if (level == VideoGiftInfo.SUPER_ADVANCED) {
//            init(advance);
//        } else if (level == VideoGiftInfo.SUPER_RICH) {
//            init(superAdvance);
//        }
//    }
//
//    public void setGiftAndComboLevel(int giftLevel, int comboLevel) {
//        if (comboLevel == 2) {
//            init(comboLevel2);
//        } else {
//            setGiftLevel(giftLevel);
//        }
//    }
//
//    private void resetAnim() {
//        if (cancellable != null) {
//            LottieUtils.cancelAnima(this);
//            cancellable.cancel();
//            cancellable = null;
//        }
//    }
//
//    @Override
//    protected void onDetachedFromWindow() {
//        resetAnim();
//        super.onDetachedFromWindow();
//    }
//}
