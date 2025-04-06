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
import org.geotools.geometry.GeneralEnvelope;
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
import org.opengis.metadata.citation.Citation;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.media.jai.PlanarImage;
import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
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
    String filePath = "E:\\RS_DATA\\LC81290392021036LGN00.tar\\LC08_L1TP_129039_20210205_20210304_01_T1_B5.TIF";
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
    public void metadata() throws Exception {
        Format format = reader.getFormat();
        GridCoverage2D gridCoverage2D = reader.read(null);

        // 1. 地理范围
        GeneralEnvelope envelope = reader.getOriginalEnvelope();
        System.out.println("envelope = " + envelope);

        // 2. 网格范围
        GridEnvelope range = reader.getOriginalGridRange();
        int width = range.getSpan(0);
        int height = range.getSpan(1);
        System.out.println("width = " + width);
        System.out.println("height = " + height);

        // 3. 空间分辨率（像元大小）
        double resX = envelope.getSpan(0) / width;
        double resY = envelope.getSpan(1) / height;
        System.out.println("Resolution: " + resX + " x " + resY);

        /**
         * =======================
         * == 4. 坐标参考系（投影）==
         * =======================
         */
        // 获取投影信息（CRS）
        CoordinateReferenceSystem crs = reader.getCoordinateReferenceSystem();
        System.out.println("CRS Name: " + crs.getName().getCode());
        // 判断是否是投影坐标系
        boolean isProjected = CRS.getProjectedCRS(crs) != null;
        System.out.println("Is Projected: " + isProjected);
        // 尝试获取 EPSG Code
        //        🧠 小技巧：EPSG Code 为何重要？
        //        EPSG:4326 = WGS84 地理坐标系（经纬度）
        //        EPSG:3857 = Web Mercator（在线地图）
        //        EPSG:32648 = WGS84 UTM 投影第 48N 带
        //        EPSG:4490 = CGCS2000 地理坐标系（国产 WGS84）
        //        你可以据此判断数据是否需要「投影转换」，或者是否能直接叠加在高德/天地图上。
        Integer epsg = CRS.lookupEpsgCode(crs, true);
        System.out.println("EPSG Code: " + (epsg != null ? epsg : "未知或无效"));

        // 5. 获取波段数
        RenderedImage renderedImage = gridCoverage2D.getRenderedImage();
        SampleModel sampleModel = renderedImage.getSampleModel();
        int numBands = sampleModel.getNumBands();
        System.out.println("numBands = " + numBands);

        // 6. 获取像元类型
        int dataType = sampleModel.getDataType();
        String typeStr;
        switch (dataType) {
            case java.awt.image.DataBuffer.TYPE_BYTE:
                typeStr = "Byte (8-bit)";
                break;
            case java.awt.image.DataBuffer.TYPE_USHORT:
                typeStr = "Unsigned Short (16-bit)";
                break;
            case java.awt.image.DataBuffer.TYPE_SHORT:
                typeStr = "Signed Short (16-bit)";
                break;
            case java.awt.image.DataBuffer.TYPE_INT:
                typeStr = "Integer (32-bit)";
                break;
            case java.awt.image.DataBuffer.TYPE_FLOAT:
                typeStr = "Float32 (单精度浮点)";
                break;
            case java.awt.image.DataBuffer.TYPE_DOUBLE:
                typeStr = "Float64 (双精度浮点)";
                break;
            default:
                typeStr = "未知类型";
        }
        System.out.println("像元数据类型: " + typeStr);

        // 7. 提取颜色
        ColorModel colorModel = renderedImage.getColorModel();
        if (colorModel instanceof IndexColorModel) {
            IndexColorModel icm = (IndexColorModel) colorModel;
            int mapSize = icm.getMapSize();

            for (int i = 0; i < mapSize; i++) {
                int r = icm.getRed(i);
                int g = icm.getGreen(i);
                int b = icm.getBlue(i);
                System.out.println("Pixel value " + i + " -> RGB(" + r + "," + g + "," + b + ")");
            }
        } else {
            System.out.println("This image does not use a color palette.");
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
        String SOURCE_FILE_PATH = "E:\\\\遥感数据\\\\NE2_50M_SR\\\\NE2_50M_SR.tif";
        String TARGET_FILE_PATH = "E:\\\\遥感数据\\\\NE2_50M_SR";
        String TARGET_FILE_NAME = "crop.tif";

        File file = new File(SOURCE_FILE_PATH);
        AbstractGridFormat format = GridFormatFinder.findFormat(file);
        // 修复GeoTiff加载时的坐标轴顺序问题
        Hints hints = null;
        if (format instanceof GeoTiffFormat) {
            hints = new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE);
        }

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
