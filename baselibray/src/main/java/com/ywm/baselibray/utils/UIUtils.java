package com.ywm.baselibray.utils;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;

import androidx.annotation.ColorInt;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.Method;

/**
 * Project
 */
public class UIUtils {
    public static Float sScreenDensity = null;
    public static Float sTextScale = null;

    public static int SCREEN_HEIGHT;
    public static int SCREEN_WIDTH;

    /**
     * 获取手机屏幕刷新率
     *
     * @return 刷新率
     */
    public static float getRefreshRate() {
        WindowManager w = (WindowManager) Utils.getApp().getSystemService(Context.WINDOW_SERVICE);
        if (w == null) {
            return 0F;
        }
        Display display = w.getDefaultDisplay();
        return display.getRefreshRate();
    }

    /**
     * 获得Toolbar的高度
     *
     * @param pContext
     * @return
     */
    public static int getToolbarHeight(Context pContext) {
        // Calculate ActionBar height
        int actionBarHeight = 0;
        TypedValue tv = new TypedValue();
        if (pContext.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, pContext.getResources().getDisplayMetrics());
        }
        if (actionBarHeight <= 0) {
            final float scale = pContext.getResources().getDisplayMetrics().density;
            return (int) (52f * scale + 0.5f);
        }
        return actionBarHeight;
    }

    /**
     * 获取ListView的滚动距离，此方法不具有通用性，要求ListView中的条目高度必须一样才行
     *
     * @param listView
     * @param headerHeight 如果有header，需要加上header的高度,否则，传0即可
     * @return
     */
    public static int getScrollY(AbsListView listView, int headerHeight) {
        View c = listView.getChildAt(0);
        if (c == null) {
            return 0;
        }
        int firstVisiblePosition = listView.getFirstVisiblePosition();
        int top = c.getTop();

        int extraHeight = 0;
        if (firstVisiblePosition >= 1) {
            extraHeight = headerHeight;
        }
        return -top + firstVisiblePosition * c.getHeight() + extraHeight;
    }

    public static float getScreenDensity() {
        if (sScreenDensity == null) {
            sScreenDensity = getDisplayMetrics().density;
        }
        return sScreenDensity.floatValue();
    }


    public static boolean isPortrait() {
        WindowManager w = (WindowManager) Utils.getApp().getSystemService(Context.WINDOW_SERVICE);
        Display d = w.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        d.getMetrics(metrics);
        int widthPixels = metrics.widthPixels;
        int heightPixels = metrics.heightPixels;
        return widthPixels < heightPixels;
    }

    /**
     * 获取屏幕分辨率宽度 *
     * 解决横屏切换竖屏后，屏幕宽度获取错误
     */
    public static int getScreenWidthPortrait() {
        return Math.min(getScreenWidth(), getScreenHeight());
    }

    /**
     * 获取屏幕分辨率宽度 *
     */
    public static int getScreenWidth() {
        getScreenSize();
        return SCREEN_WIDTH;
    }

    /**
     * 获取屏幕分辨率高度 *
     */
    public static int getScreenHeight() {
        getScreenSize();
        return SCREEN_HEIGHT;
    }

    public static void getScreenSize() {
        if (SCREEN_HEIGHT != 0 && SCREEN_WIDTH != 0) {
            return;
        }

        if (Utils.getApp() == null) {
            return;
        }

        WindowManager w = (WindowManager) Utils.getApp().getSystemService(Context.WINDOW_SERVICE);
        Display d = w.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        d.getMetrics(metrics);
        int widthPixels = metrics.widthPixels;
        int heightPixels = metrics.heightPixels;

        try {
            Point realSize = new Point();
            Display.class.getMethod("getRealSize", Point.class).invoke(d, realSize);
            widthPixels = realSize.x;
            heightPixels = realSize.y;
        } catch (Exception ignored) {
        }
        SCREEN_WIDTH = widthPixels;
        SCREEN_HEIGHT = heightPixels;
    }

    public static Resources getResources() {
        return Utils.getApp().getResources();
    }

    public static DisplayMetrics getDisplayMetrics() {
        return getResources().getDisplayMetrics();
    }

    public static float getTextScale() {
        if (sTextScale == null) {
            sTextScale = getDisplayMetrics().scaledDensity;
        }
        return sTextScale.floatValue();
    }

    public static String getString(int resource) {
        return Utils.getApp().getString(resource);
    }

    public static String getString(int resource, Object... formatArgs) {
        return Utils.getApp().getString(resource, formatArgs);
    }

    public static int getColor(int resource) {
        return getResources().getColor(resource);
    }

    public static int getPixels(float dip) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, getDisplayMetrics()));
    }

    /**
     * 像素转换成DIP *
     */
    public static int getDips(float pixel) {
        if (pixel >= 0) {
            return (int) (pixel / getDisplayMetrics().density + 0.5f);
        } else {
            return 0;
        }
    }

    public static int getDimensionPixelSize(int id) {
        return getResources().getDimensionPixelSize(id);
    }

    public static int sp2pix(float sp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getDisplayMetrics()));
    }

    public static int px2sp(float px) {
        float sp = px / getResources().getDisplayMetrics().scaledDensity;
        return (int) sp;
    }

    /**
     * 解析图片文件，返回图片对象。根据设备自动调整图片大小
     *
     * @param file
     * @return
     */
    public static Bitmap decodeResourceBitmap(File file, int resid) {
        if (!file.exists()) {
            return null;
        }
        try {
            return decodeResourceBitmap(new FileInputStream(file), resid);
        } catch (FileNotFoundException e) {
        }
        return null;
    }

    public static Bitmap decodeResourceBitmap(InputStream is, int resId) {
        try {
            //            if (resId < 0) {
            //                resId = R.drawable.app_icon;
            //            }

            Rect pad = new Rect();
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inScreenDensity = getDisplayMetrics().densityDpi;
            TypedValue value = new TypedValue();
            Resources resources = getResources();
            resources.getValue(resId, value, false);

            final int density = value.density;
            if (density == TypedValue.DENSITY_DEFAULT) {
                opts.inDensity = DisplayMetrics.DENSITY_DEFAULT;
            } else if (density != TypedValue.DENSITY_NONE) {
                opts.inDensity = density;
            }
            opts.inTargetDensity = resources.getDisplayMetrics().densityDpi;

            Bitmap bitmap = BitmapFactory.decodeStream(is, pad, opts);
            is.close();
            return bitmap;
        } catch (Throwable e) {
        }

        return null;
    }

    public static InputMethodManager getInputMethodManager() {
        return (InputMethodManager) Utils.getApp().getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    public static void hideInputMethod(Activity activity) {
        InputMethodManager im = ((InputMethodManager) Utils.getApp().getSystemService(Activity.INPUT_METHOD_SERVICE));
        View curFocusView = activity.getCurrentFocus();
        if (curFocusView != null) {
            im.hideSoftInputFromWindow(curFocusView.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    public static RectF getViewLocationOnScreen(View v) {
        int[] location = new int[2];
        v.getLocationOnScreen(location);
        int vx = location[0];
        int vy = location[1];
        RectF viewRectF = new RectF(vx, vy, vx + v.getMeasuredWidth(), vy + v.getMeasuredHeight());
        return viewRectF;
    }

    public static void switchFullscreen(Activity activity, boolean isFullscreen) {
        if (isFullscreen) {
            WindowManager.LayoutParams attrs = activity.getWindow().getAttributes();
            attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
            activity.getWindow().setAttributes(attrs);
        } else {
            WindowManager.LayoutParams attrs = activity.getWindow().getAttributes();
            attrs.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
            activity.getWindow().setAttributes(attrs);
        }
    }

    /**
     * 生成新的resId,同时不与之前的重复
     * <p>
     * Each resource identifier consists of 32 bits were 8 top bits defines the package
     * in which the resource belongs to.
     * Platform resources are 0x01 and 0x7F are application resources.
     * The idea is to use the remaining 253 values for extension packages.
     *
     * @param res       旧的resId
     * @param newOffset 生成新的resId的offset,最多支持生成253个不同
     * @return
     */
    public static long uniqueResId(long res, long newOffset) {
        if (newOffset <= 0 || newOffset >= 0x000000fd) return res;
        if (res >> 24 == 0x0000007f || res >> 24 == 0x00000001) {
            //0x7f -> 0x80
            //0x01 -> 0x02
            if (newOffset >= 0x0000007e) newOffset = newOffset + 1;
            return res + (newOffset << 24);
        } else {
            //other
            return res;
        }
    }

    /**
     * 获取虚拟按键高度
     *
     * @return
     */
    public static int getVirtualBarHeight() {
        return getRealScreenHeight() - getScreenHeight();
    }

    /**
     * 获取真正屏幕高度（若有虚拟按键，会加上虚拟按键）
     *
     * @return
     */
    public static int getRealScreenHeight() {
        int h = 0;
        WindowManager windowManager = (WindowManager) Utils.getApp().getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        try {
            Class c = Class.forName("android.view.Display");
            Method method = c.getMethod("getRealMetrics", DisplayMetrics.class);
            method.invoke(display, dm);
            h = dm.heightPixels;
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (h <= 0)
            h = getScreenHeight();
        return h;
    }

    public static void screenRotateLeft(View v, int px, int py) {
        final int[] loc = new int[2];
        v.getLocationOnScreen(loc);
        final int vh = v.getHeight();
        final int top = loc[1];
        final int sw = getScreenWidth();
        final int sh = getRealScreenHeight();
        final int centerY = top + (vh >> 1);
        v.setRotation(90);
        v.setTranslationY(py - centerY);
        int tx = sw - px - (vh >> 1) - sw * top / sh;
        v.setTranslationX(tx);
    }

    public static void screenRotateLeftByCenter(View v) {
        screenRotateLeft(v, getScreenWidth() >> 1, getRealScreenHeight() >> 1);
    }

    public static void screenRotateNormal(View v) {
        v.setRotation(0);
        v.setTranslationY(0);
        v.setTranslationX(0);
    }

    public static Drawable getCornerBackGroundDrawable(int corner, @ColorInt int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(corner);
        drawable.setColor(color);
        return drawable;
    }

    public static int calculateTextWidth(String text, float sizeInSp) {
        TextPaint textPaint = new TextPaint();
        textPaint.setTextSize(UIUtils.sp2pix(sizeInSp));
        Rect bounds = new Rect();
        textPaint.getTextBounds(text, 0, text.length(), bounds);

        return bounds.width();
    }

    /**
     * 只能用于Activity，其他情况，如Fragment，请使用{@link #findView(View, int)}
     */
    @SuppressWarnings("unchecked")
    public static <T extends View> T findView(@NonNull Activity activity, @IdRes int id) {
        return (T) activity.findViewById(id);
    }

    @SuppressWarnings("unchecked")
    public static <T extends View> T findView(@NonNull View view, @IdRes int id) {
        return (T) view.findViewById(id);
    }

    /**
     * 计算单个 Item 扣除各种间距后的宽度
     *
     * @param singleItemIntervalTotal 单个 Item 横向上的padding、margin和
     * @param otherIntervalTotal      其他间隙，比如横向上整体的。
     * @param columnsNum              列数
     * @return
     */
    public static int calculateItemWidthOnScreen(int singleItemIntervalTotal, int otherIntervalTotal, int columnsNum) {
        return (UIUtils.getScreenWidth() - singleItemIntervalTotal * columnsNum - otherIntervalTotal) / columnsNum;
    }

    /**
     * 重设 View 的宽高，宽高不变时直接返回。
     *
     * @param view
     * @param newHeight
     * @param newWidth
     * @return view
     */
    public static <T extends View> T resetViewSize(T view, int newHeight, int newWidth) {
        int oldHeight = view.getMeasuredHeight();
        int oldWidth = view.getMeasuredWidth();
        if (oldHeight == newHeight && oldWidth == newWidth) {
            return view;
        }
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.height = newHeight;
        layoutParams.width = newWidth;
        view.requestLayout();
        return view;
    }

    public static int getStatuBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    /**
     * 是否18：9
     *
     * @return
     */
    public static boolean isFullScreen() {
        return getScreenHeight() / getScreenWidth() >= 2;
    }

    /**
     * 根据alpha值获取 透明颜色
     *
     * @param alpha
     * @param color 颜色值 不带#
     * @return
     */
    public static String getAlphaColor(float alpha, String color) {
        if (alpha > 1f) {
            alpha = 1f;
        }
        if (alpha < 0f) {
            alpha = 0f;
        }
        StringBuilder alphaColor = new StringBuilder();
        alphaColor.append("#");
        int alpha10 = (int) (255 * alpha);
        String alpha16 = Integer.toHexString(alpha10);

        if (alpha16.length() == 1) {
            alphaColor.append("0");
        }
        alphaColor.append(alpha16);
        alphaColor.append(color);
        return alphaColor.toString();
    }

    /**
     * 获取 颜色
     *
     * @param alpha 透明度
     * @param red   红色值
     * @param green 绿色值
     * @param blue  蓝色值
     * @return color
     */
    public static int argb(float alpha, float red, float green, float blue) {
        return ((int) (alpha * 255.0f + 0.5f) << 24) |
                ((int) (red * 255.0f + 0.5f) << 16) |
                ((int) (green * 255.0f + 0.5f) << 8) |
                (int) (blue * 255.0f + 0.5f);
    }

    /**
     * Convert a translucent themed Activity
     * {@link android.R.attr#windowIsTranslucent} to a fullscreen opaque
     * Activity.
     * <p>
     * Call this whenever the background of a translucent Activity has changed
     * to become opaque. Doing so will allow the {@link android.view.Surface} of
     * the Activity behind to be released.
     * <p>
     * This call has no effect on non-translucent activities or on activities
     * with the {@link android.R.attr#windowIsFloating} attribute.
     */
    public static void convertActivityFromTranslucent(Activity activity) {
        try {
            Method method = Activity.class.getDeclaredMethod("convertFromTranslucent");
            method.setAccessible(true);
            method.invoke(activity);
        } catch (Throwable t) {
            LogUtils.e(t.toString());
        }
    }

    /**
     * Convert a translucent themed Activity
     * {@link android.R.attr#windowIsTranslucent} back from opaque to
     * translucent following a call to
     * {@link #convertActivityFromTranslucent(Activity)} .
     * <p>
     * Calling this allows the Activity behind this one to be seen again. Once
     * all such Activities have been redrawn
     * <p>
     * This call has no effect on non-translucent activities or on activities
     * with the {@link android.R.attr#windowIsFloating} attribute.
     */
    public static void convertActivityToTranslucent(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            convertActivityToTranslucentAfterL(activity);
        } else {
            convertActivityToTranslucentBeforeL(activity);
        }
    }

    /**
     * Calling the convertToTranslucent method on platforms before Android 5.0
     */
    private static void convertActivityToTranslucentBeforeL(Activity activity) {
        try {
            Class<?>[] classes = Activity.class.getDeclaredClasses();
            Class<?> translucentConversionListenerClazz = null;
            for (Class<?> clazz : classes) {
                if (clazz.getSimpleName().contains("TranslucentConversionListener")) {
                    translucentConversionListenerClazz = clazz;
                }
            }
            Method method = Activity.class.getDeclaredMethod("convertToTranslucent",
                    translucentConversionListenerClazz);
            method.setAccessible(true);
            method.invoke(activity, new Object[]{
                    null
            });
        } catch (Throwable t) {
            LogUtils.e(t.toString());
        }
    }

    /**
     * Calling the convertToTranslucent method on platforms after Android 5.0
     */
    private static void convertActivityToTranslucentAfterL(Activity activity) {
        try {
            Method getActivityOptions = Activity.class.getDeclaredMethod("getActivityOptions");
            getActivityOptions.setAccessible(true);
            Object options = getActivityOptions.invoke(activity);

            Class<?>[] classes = Activity.class.getDeclaredClasses();
            Class<?> translucentConversionListenerClazz = null;
            for (Class<?> clazz : classes) {
                if (clazz.getSimpleName().contains("TranslucentConversionListener")) {
                    translucentConversionListenerClazz = clazz;
                }
            }
            Method convertToTranslucent = Activity.class.getDeclaredMethod("convertToTranslucent",
                    translucentConversionListenerClazz, ActivityOptions.class);
            convertToTranslucent.setAccessible(true);
            convertToTranslucent.invoke(activity, null, options);
        } catch (Throwable t) {
            LogUtils.e(t.toString());
        }
    }

    public static boolean isViewRtl(View view) {
        return ViewCompat.getLayoutDirection(view) == ViewCompat.LAYOUT_DIRECTION_RTL;
    }
    public static boolean isRtl(Context mcontext) {
        if (mcontext == null) {
            return false;
        }
        return  mcontext.getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
    }


    /**
     * 增加一个View的触摸区域
     *
     * @param view
     * @param top
     * @param bottom
     * @param left
     * @param right
     */
    public static void expandViewTouchDelegate(final View view, final int top, final int bottom, final int left, final int right) {

        ((View) view.getParent()).post(new Runnable() {
            @Override
            public void run() {
                Rect bounds = new Rect();
                view.setEnabled(true);
                view.getHitRect(bounds);

                bounds.top -= top;
                bounds.bottom += bottom;
                bounds.left -= left;
                bounds.right += right;

                TouchDelegate touchDelegate = new TouchDelegate(bounds, view);

                if (View.class.isInstance(view.getParent())) {
                    ((View) view.getParent()).setTouchDelegate(touchDelegate);
                }
            }
        });
    }

    /**
     * 增加一个view的触摸区域
     *
     * @param view
     * @param expandTouchWidth
     */
    public static void expandViewTouchDelegate(final View view, final int expandTouchWidth) {
        final View parent = (View) view.getParent();
        parent.post(() -> {
            final Rect r = new Rect();
            view.getHitRect(r);
            r.top -= expandTouchWidth;
            r.bottom += expandTouchWidth;
            r.left -= expandTouchWidth;
            r.right += expandTouchWidth;
            parent.setTouchDelegate(new TouchDelegate(r, view));
        });
    }
}
