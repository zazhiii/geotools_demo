import com.sun.media.jai.codecimpl.util.RasterFactory;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.coverage.grid.io.imageio.geotiff.GeoKeyEntry;
import org.geotools.coverage.grid.io.imageio.geotiff.GeoTiffIIOMetadataDecoder;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.data.DataSourceException;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.GridCoverageLayer;
import org.geotools.map.MapContent;
import org.geotools.referencing.CRS;
import org.geotools.swing.JMapFrame;
import org.geotools.util.factory.Hints;
import org.junit.Test;
import org.opengis.coverage.grid.Format;
import org.opengis.coverage.grid.GridCoordinates;
import org.opengis.coverage.grid.GridCoverageWriter;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.media.jai.PlanarImage;
import javax.swing.*;
import java.awt.*;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

/**
 * @author zazhi
 * @date 2025/3/15
 * @description: TODO
 */
public class TifTest {
    String filePath = "E:\\遥感数据\\LE07_L1TP_129039_20210723_20210723_01\\LE07_L1TP_129039_20210723_20210723_01_RT_B1.TIF";
//    String filePath = "http://localhost:9090/browser/minio-upload-demo/2025-03-23%2F93c2c6cf-ac06-4eba-9371-0525dcf4b2c0.TIF";
    File tiffFile = new File(filePath);
    GeoTiffReader reader;
    {
        try {
            reader = new GeoTiffReader(tiffFile);
        } catch (DataSourceException e) {
            throw new RuntimeException("读取文件失败");
        }
    }



    @Test
    public void readTifInfo() throws Exception {

        // GridEnvelope: 网格的结构
        GridEnvelope originalGridRange = reader.getOriginalGridRange();

        // dimension: 维度
        int dimension = originalGridRange.getDimension();

        // GridCoordinates: 包含各个维度的坐标信息
        GridCoordinates highCoordinates = originalGridRange.getHigh();
        int[] maxCoordinates = highCoordinates.getCoordinateValues(); // 横纵坐标的最大值：[x, y](二维)
        GridCoordinates minCoordinates = originalGridRange.getLow();
        int[] minCoordinateValues = minCoordinates.getCoordinateValues(); // 横纵坐标的最小值：[x, y](二维)
        int w = maxCoordinates[0] + 1; // 宽度
        int h = maxCoordinates[1] + 1; // 高度

        // 元数据
        GeoTiffIIOMetadataDecoder metadata = reader.getMetadata();
        // GeoKey是GeoTIFF中存储地理参考信息的键值对（例如坐标系、投影参数等）。
        Collection<GeoKeyEntry> geoKeys = metadata.getGeoKeys();
//        geoKeys.forEach(System.out::println);

        Format format = reader.getFormat();
        System.out.println(format.getDescription());
        System.out.println(format.getDocURL());
        System.out.println(format.getName());
        System.out.println(format.getVendor());
        System.out.println(format.getVersion());
    }

    /**
     * 裁剪TIF文件
     */
    @Test
    public void cropTif() throws Exception {
        String SOURCE_FILE_PATH = "E:\\遥感数据\\NE2_50M_SR\\NE2_50M_SR.tif";
        String TARGET_FILE_PATH = "E:\\遥感数据\\NE2_50M_SR";
        String TARGET_FILE_NAME = "crop.tif";

        File file = new File(SOURCE_FILE_PATH);
        AbstractGridFormat format = GridFormatFinder.findFormat(file);

        // 修复GeoTiff加载时的坐标轴顺序问题
        Hints hints = null;
//        if (format instanceof GeoTiffFormat) {
//            hints = new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE);
//        }

        GridCoverage2DReader reader = format.getReader(file, hints);
        GridCoverage2D gridCoverage = reader.read(null);

        Envelope2D coverageEnvelope2D = gridCoverage.getEnvelope2D();
        double minX = coverageEnvelope2D.getMinX();
        double maxX = coverageEnvelope2D.getMaxX();
        double minY = coverageEnvelope2D.getMinY();
        double maxY = coverageEnvelope2D.getMaxY();
        System.out.println("minX: " + minX);
        System.out.println("maxX: " + maxX);
        System.out.println("minY: " + minY);
        System.out.println("maxY: " + maxY);

        // 获取栅格数据的坐标参考系统
        CoordinateReferenceSystem targetCRS = gridCoverage.getCoordinateReferenceSystem();
        System.out.println(targetCRS.getName());
        // 创建分块的范围
        // (x1, y1)为左下角坐标，(x2, y2)为右上角坐标
        int x1 = -180, x2 = 0, y1 = -90, y2 = 0;
        ReferencedEnvelope envelope = new ReferencedEnvelope(x1, x2, y1, y2, targetCRS);
        // 创建处理器
        CoverageProcessor processor = CoverageProcessor.getInstance();
        //
        ParameterValueGroup param = processor.getOperation("CoverageCrop").getParameters();
        param.parameter("Source").setValue(gridCoverage);
        param.parameter("Envelope").setValue(envelope);
        GridCoverage2D finalCoverage = (GridCoverage2D) processor.doOperation(param);


        File tileDirectory = new File(TARGET_FILE_PATH);
        if (!tileDirectory.exists()) {
            tileDirectory.mkdirs();
        }

        // 输出文件
        File tileFile = new File(tileDirectory, TARGET_FILE_NAME);
        format.getWriter(tileFile).write(finalCoverage, null);
    }
}
