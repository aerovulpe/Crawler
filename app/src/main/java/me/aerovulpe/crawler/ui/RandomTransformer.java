package me.aerovulpe.crawler.ui;

import android.view.View;

import com.ToxicBakery.viewpager.transforms.ABaseTransformer;
import com.ToxicBakery.viewpager.transforms.AccordionTransformer;
import com.ToxicBakery.viewpager.transforms.BackgroundToForegroundTransformer;
import com.ToxicBakery.viewpager.transforms.CubeInTransformer;
import com.ToxicBakery.viewpager.transforms.CubeOutTransformer;
import com.ToxicBakery.viewpager.transforms.DefaultTransformer;
import com.ToxicBakery.viewpager.transforms.DepthPageTransformer;
import com.ToxicBakery.viewpager.transforms.FlipHorizontalTransformer;
import com.ToxicBakery.viewpager.transforms.ForegroundToBackgroundTransformer;
import com.ToxicBakery.viewpager.transforms.RotateDownTransformer;
import com.ToxicBakery.viewpager.transforms.RotateUpTransformer;
import com.ToxicBakery.viewpager.transforms.ScaleInOutTransformer;
import com.ToxicBakery.viewpager.transforms.StackTransformer;
import com.ToxicBakery.viewpager.transforms.TabletTransformer;
import com.ToxicBakery.viewpager.transforms.ZoomInTransformer;
import com.ToxicBakery.viewpager.transforms.ZoomOutSlideTransformer;
import com.ToxicBakery.viewpager.transforms.ZoomOutTranformer;

import static android.support.v4.view.ViewPager.PageTransformer;

/**
 * Created by Aaron on 24/06/2015.
 */
public class RandomTransformer implements PageTransformer {

    private PageTransformer mTransformerImpl;

    public RandomTransformer() {
        mTransformerImpl = getABaseTransformer();
    }

    @Override
    public void transformPage(View page, float position) {
        if (position == 0)
            mTransformerImpl = getABaseTransformer();

        mTransformerImpl.transformPage(page, position);
    }

    private ABaseTransformer getABaseTransformer() {
        ABaseTransformer transformerImpl;
        switch ((int) (Math.random() * 16)) {
            case 0:
                transformerImpl = new DefaultTransformer();
                break;
            case 1:
                transformerImpl = new AccordionTransformer();
                break;
            case 2:
                transformerImpl = new BackgroundToForegroundTransformer();
                break;
            case 3:
                transformerImpl = new CubeInTransformer();
                break;
            case 4:
                transformerImpl = new CubeOutTransformer();
                break;
            case 5:
                transformerImpl = new DepthPageTransformer();
                break;
            case 6:
                transformerImpl = new FlipHorizontalTransformer();
                break;
            case 7:
                transformerImpl = new ForegroundToBackgroundTransformer();
                break;
            case 8:
                transformerImpl = new RotateDownTransformer();
                break;
            case 9:
                transformerImpl = new RotateUpTransformer();
                break;
            case 10:
                transformerImpl = new ScaleInOutTransformer();
                break;
            case 11:
                transformerImpl = new StackTransformer();
                break;
            case 12:
                transformerImpl = new TabletTransformer();
                break;
            case 13:
                transformerImpl = new ZoomInTransformer();
                break;
            case 14:
                transformerImpl = new ZoomOutSlideTransformer();
                break;
            case 15:
                transformerImpl = new ZoomOutTranformer();
                break;
            default:
                transformerImpl = new DefaultTransformer();
        }
        return transformerImpl;
    }
}
