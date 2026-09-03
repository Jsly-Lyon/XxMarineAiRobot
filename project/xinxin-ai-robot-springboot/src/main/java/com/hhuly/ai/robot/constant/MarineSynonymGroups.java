package com.hhuly.ai.robot.constant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 海洋科学领域同义词表（84 组）
 *
 * @author: li
 * @date: 2026/9/3
 * @description: 每组为“语义等价”的一组写法（含中英/简称/异名）。
 * 用法：检索时把 query 中命中本表的 token 展开为组内 OR 链（扩大关键词召回），
 * 并据此生成同义改写变体做多查询分解。
 **/
public final class MarineSynonymGroups {

    private MarineSynonymGroups() {
    }

    /** 84 组同义词（每组 2~4 个等价写法） */
    public static final List<List<String>> GROUPS = List.of(
            List.of("海洋", "大海", "海域", "ocean"),
            List.of("洋流", "海流", "海洋环流", "ocean current"),
            List.of("黑潮", "日本暖流", "kuroshio"),
            List.of("上升流", "涌升流", "upwelling"),
            List.of("潮汐", "潮", "tide"),
            List.of("潮间带", "潮滩", "滩涂", "intertidal"),
            List.of("海冰", "浮冰", "sea ice"),
            List.of("极地海冰", "极冰", "polar ice"),
            List.of("温跃层", "温度跃层", "thermocline"),
            List.of("盐度", "含盐量", "salinity"),
            List.of("水团", "水体", "water mass"),
            List.of("水色", "海水颜色", "ocean color"),
            List.of("浊度", "浑浊度", "turbidity"),
            List.of("环流", "大洋环流", "gyre"),
            List.of("风海流", "风生流", "wind-driven current"),
            List.of("密度流", "热盐环流", "thermohaline circulation"),
            List.of("赤潮", "有害藻华", "有害赤潮", "red tide"),
            List.of("水华", "藻华", "algal bloom"),
            List.of("富营养化", "水体富营养", "eutrophication"),
            List.of("营养盐", "营养物质", "nutrient"),
            List.of("浮游植物", "浮游藻", "phytoplankton", "微藻"),
            List.of("浮游动物", "zooplankton"),
            List.of("微型浮游生物", "微微型浮游", "picoplankton"),
            List.of("游泳动物", "自游生物", "nekton"),
            List.of("底栖生物", "底栖", "benthos"),
            List.of("头足类", "cephalopod"),
            List.of("甲壳类", "crustacean"),
            List.of("贝类", "双壳类", "软体动物", "shellfish"),
            List.of("鱼类", "鱼", "fish"),
            List.of("珊瑚", "珊瑚虫", "coral"),
            List.of("珊瑚礁", "珊瑚礁生态", "coral reef"),
            List.of("珊瑚白化", "白化", "bleaching"),
            List.of("红树林", "mangrove"),
            List.of("海草床", "海草", "seagrass"),
            List.of("滨海湿地", "沿海湿地", "coastal wetland"),
            List.of("河口", "河口湾", "estuary"),
            List.of("三角洲", "delta"),
            List.of("海湾", "港湾", "gulf"),
            List.of("潟湖", "泻湖", "lagoon"),
            List.of("海沟", "深渊", "trench"),
            List.of("大陆架", "陆架", "shelf"),
            List.of("陆坡", "大陆坡", "slope"),
            List.of("深海", "深水", "deep sea"),
            List.of("热液喷口", "深海热液", "hydrothermal vent"),
            List.of("冷泉", "海底冷泉", "cold seep"),
            List.of("海山", "海底山", "seamount"),
            List.of("海底地形", "海底地貌", "bathymetry"),
            List.of("沉积物", "底质", "sediment"),
            List.of("海洋酸化", "酸化", "acidification"),
            List.of("低氧区", "缺氧区", "死区", "hypoxia"),
            List.of("微塑料", "塑料微粒", "microplastic"),
            List.of("海洋垃圾", "塑料垃圾", "marine debris"),
            List.of("海洋噪声", "水下噪声", "ocean noise"),
            List.of("海水污染", "海洋污染", "marine pollution"),
            List.of("溢油", "石油泄漏", "oil spill"),
            List.of("厄尔尼诺", "厄尔尼诺现象", "enso", "el nino"),
            List.of("拉尼娜", "la nina"),
            List.of("季风", "monsoon"),
            List.of("台风", "热带气旋", "typhoon"),
            List.of("海浪", "风浪", "wave"),
            List.of("风暴潮", "storm surge"),
            List.of("海啸", "tsunami"),
            List.of("内波", "内孤立波", "internal wave"),
            List.of("海平面上升", "海面上升", "sea level rise"),
            List.of("海岸侵蚀", "岸线侵蚀", "coastal erosion"),
            List.of("渔业", "捕捞业", "fishery"),
            List.of("捕捞", "捕捞作业", "fishing"),
            List.of("增殖放流", "放流", "stock enhancement"),
            List.of("过度捕捞", "捕捞过度", "overfishing"),
            List.of("兼捕", "副渔获", "bycatch"),
            List.of("水产养殖", "海水养殖", "aquaculture"),
            List.of("种群", "群体", "居群", "population"),
            List.of("洄游", "迁徙", "migration"),
            List.of("产卵场", "产卵地", "spawning ground"),
            List.of("孵化场", "孵化", "hatchery"),
            List.of("饵料", "饵料生物", "prey"),
            List.of("食物网", "食物链", "food web"),
            List.of("初级生产力", "光合生产力", "primary productivity"),
            List.of("次级生产", "次生生产", "secondary production"),
            List.of("生态位", "生态位分化", "niche"),
            List.of("生物多样性", "多样性", "biodiversity"),
            List.of("外来种", "外来物种", "invasive species"),
            List.of("海洋保护区", "保护区", "marine protected area"),
            List.of("遥感", "卫星遥感", "remote sensing"),
            List.of("海况", "海况等级", "sea state"),
            // ===== 追加扩充（按主题） =====
            // 物理海洋
            List.of("涌浪", "涌", "swell"),
            List.of("潮差", "潮位差", "tidal range"),
            List.of("半日潮", "半日分潮"),
            List.of("沿岸流", "岸流", "coastal current"),
            List.of("离岸流", "裂流", "rip current"),
            List.of("波浪能", "波能", "wave energy"),
            List.of("海表温度", "海温", "sst", "sea surface temperature"),
            List.of("南极绕极流", "西风漂流", "antarctic circumpolar current"),
            List.of("西边界流", "边界流"),
            List.of("中尺度涡", "涡旋", "mesoscale eddy"),
            List.of("冷涡", "气旋式涡旋"),
            List.of("暖涡", "反气旋式涡旋"),
            List.of("层结", "密度层结", "stratification"),
            List.of("混合层", "上混合层", "mixed layer"),
            List.of("埃克曼输运", "埃克曼流", "ekman transport"),
            List.of("地转流", "地转平衡", "geostrophic flow"),
            List.of("次表层", "次表层水", "subsurface"),
            List.of("中深层水", "中层水", "intermediate water"),
            // 化学海洋
            List.of("溶解氧", "溶氧", "do", "dissolved oxygen"),
            List.of("总碱度", "碱度", "alkalinity"),
            List.of("酸碱度", "ph值", "ph"),
            List.of("痕量金属", "微量金属", "trace metal"),
            List.of("重金属污染", "重金属", "heavy metal"),
            List.of("营养盐限制", "营养限制", "nutrient limitation"),
            List.of("氮磷比", "n/p比", "np ratio"),
            List.of("河流入海", "陆源输入", "riverine input"),
            List.of("入海通量", "河流输送", "river flux"),
            List.of("二氧化碳分压", "pco2"),
            List.of("碳酸盐体系", "碳酸系统", "carbonate system"),
            List.of("溶解无机碳", "dic"),
            List.of("颗粒有机碳", "poc"),
            List.of("溶解有机碳", "doc"),
            List.of("蓝色碳汇", "蓝碳", "blue carbon"),
            List.of("碳汇", "碳封存", "carbon sink"),
            List.of("碳循环", "海洋碳循环"),
            // 生物与生态
            List.of("微生物环", "微生物食物网", "microbial loop"),
            List.of("生物泵", "生物碳泵", "biological pump"),
            List.of("初级生产者", "自养生物", "autotroph"),
            List.of("消费者", "异养生物", "heterotroph"),
            List.of("碎屑", "碎屑物质", "detritus"),
            List.of("底栖动物", "底栖无脊椎动物"),
            List.of("微型动物", "小型底栖动物", "meiofauna"),
            List.of("幼鱼", "仔稚鱼", "juvenile fish"),
            List.of("亲体", "产卵群体", "spawning stock"),
            List.of("补充群体", "补充量", "recruitment"),
            List.of("优势种", "优势种群", "dominant species"),
            List.of("指示生物", "指示种", "indicator species"),
            List.of("珍稀海洋生物", "濒危物种", "endangered species"),
            List.of("群落演替", "生态演替", "succession"),
            List.of("群落结构", "物种组成", "community structure"),
            List.of("种间关系", "种间竞争"),
            List.of("生态连通性", "生境连通", "connectivity"),
            List.of("生境修复", "栖息地修复", "habitat restoration"),
            List.of("增殖种群", "放流群体"),
            List.of("种质资源", "水产种质"),
            // 渔业
            List.of("渔汛", "渔期", "fishing season"),
            List.of("休渔", "禁渔", "fishing ban"),
            List.of("渔获物", "渔获"),
            List.of("单位努力量", "单产努力量", "cpue"),
            List.of("渔具", "捕捞工具", "fishing gear"),
            List.of("拖网", "底拖网", "trawl"),
            List.of("刺网", "流刺网", "gillnet"),
            List.of("围网", "围捕"),
            List.of("渔场", "作业渔场", "fishing ground"),
            List.of("渔业资源评估", "资源评估", "stock assessment"),
            List.of("可捕量", "总允许捕捞量", "tac"),
            // 地质
            List.of("海洋地质", "海底地质", "marine geology"),
            List.of("板块构造", "构造板块", "plate tectonics"),
            List.of("洋中脊", "中洋脊", "mid-ocean ridge"),
            List.of("俯冲带", "俯冲", "subduction zone"),
            List.of("弧后盆地", "边缘海盆地"),
            List.of("沉积速率", "堆积速率", "sedimentation rate"),
            List.of("沉积柱", "岩芯", "sediment core"),
            List.of("钙质软泥", "钙质沉积", "calcareous ooze"),
            List.of("硅质软泥", "硅质沉积", "siliceous ooze"),
            List.of("锰结核", "多金属结核", "manganese nodule"),
            List.of("富钴结壳", "钴结壳", "cobalt crust"),
            List.of("可燃冰", "天然气水合物", "hydrate"),
            List.of("滨海砂矿", "砂矿", "placer"),
            List.of("海底热液硫化物", "多金属硫化物", "seafloor massive sulfide"),
            List.of("古海洋", "古海洋学", "paleoceanography"),
            List.of("海陆交互带", "海岸带", "coastal zone"),
            List.of("沉积物捕获器", "沉箱捕获", "sediment trap"),
            // 气候
            List.of("全球变暖", "气候变暖", "warming"),
            List.of("年代际振荡", "年代际变率", "decadal variability"),
            List.of("太平洋十年涛动", "pdo"),
            List.of("北大西洋涛动", "nao"),
            List.of("印度洋偶极子", "iod"),
            List.of("副热带高压", "副高"),
            List.of("季风爆发", "季风建立"),
            List.of("海气相互作用", "海气耦合", "air-sea interaction"),
            List.of("遥相关", "气候遥相关", "teleconnection"),
            List.of("气候态", "气候平均态", "climatology"),
            // 观测与数值
            List.of("潜标", "锚系观测", "mooring"),
            List.of("漂流浮标", "浮标", "drifter"),
            List.of("剖面仪", "温盐深仪", "ctd"),
            List.of("声学多普勒流速剖面仪", "adcp"),
            List.of("水下机器人", "rov"),
            List.of("自主水下潜航器", "auv"),
            List.of("无人机", "uav"),
            List.of("数据同化", "资料同化", "data assimilation"),
            List.of("数值模式", "数值模拟", "numerical model"),
            List.of("再分析资料", "再分析", "reanalysis"),
            List.of("卫星高度计", "高度计", "altimeter"),
            List.of("雷达高度计", "微波散射计", "scatterometer"),
            List.of("海面风场", "海表风场"),
            List.of("遥感反演", "反演算法", "retrieval"),
            // 污染与治理
            List.of("污水排海", "排污口", "sewage discharge"),
            List.of("持久性有机污染物", "pops"),
            List.of("多氯联苯", "pcbs"),
            List.of("多环芳烃", "pahs"),
            List.of("有机锡", "三丁基锡"),
            List.of("生物富集", "生物累积", "bioaccumulation"),
            List.of("食物链富集", "生物放大", "biomagnification"),
            List.of("沉积物污染", "底泥污染"),
            List.of("压载水", "压舱水", "ballast water"),
            List.of("溢油污染", "油污", "oil pollution"),
            List.of("赤潮防控", "赤潮治理"),
            List.of("海洋生态修复", "生态恢复", "ecological restoration"),
            List.of("岸线修复", "海岸修复"),
            List.of("生态补偿", "生态赔偿"),
            List.of("环境容量", "环境承载力"),
            List.of("入海排污", "直排海"),
            // 管理与政策
            List.of("专属经济区", "eez"),
            List.of("领海", "领海基线", "territorial sea"),
            List.of("大陆架划界", "海域划界"),
            List.of("海洋治理", "海洋管理", "ocean governance"),
            List.of("海岸带综合管理", "海岸带管理", "iczm"),
            List.of("海洋功能区划", "海洋空间规划"),
            List.of("海域使用", "用海", "sea area use"),
            List.of("蓝色经济", "海洋经济", "blue economy"),
            List.of("海洋牧场", "人工鱼礁", "ocean ranching"),
            List.of("国家海洋战略", "海洋战略"),
            List.of("海洋保护地", "海洋公园"),
            List.of("无居民海岛", "海岛保护"),
            List.of("渔业管理", "渔政管理"),
            List.of("海上风电", "风电", "offshore wind"),
            List.of("海洋能", "海洋可再生能源"),
            List.of("海水淡化", "淡化", "desalination"),
            List.of("海水养殖业", "海洋渔业养殖"),
            List.of("深远海养殖", "深水网箱"),
            List.of("离岸工程", "海洋工程", "offshore engineering"));

    /** token -> 组下标（一次性构建，静态缓存） */
    private static final Map<String, List<String>> TOKEN_TO_GROUP = buildIndex();

    private static Map<String, List<String>> buildIndex() {
        Map<String, List<String>> index = new HashMap<>();
        for (List<String> group : GROUPS) {
            for (String token : group) {
                index.put(token.toLowerCase(), group);
            }
        }
        return index;
    }

    /**
     * 返回 token 所属同义词组（未命中返回 null）
     */
    public static List<String> groupOf(String token) {
        return TOKEN_TO_GROUP.get(token.toLowerCase());
    }

    /**
     * 把一组 token 用同义词展开：每个 token 若命中则取整个组（保留 OR 选项）
     */
    public static List<String> expanded(List<String> tokens) {
        List<String> expanded = new ArrayList<>();
        for (String token : tokens) {
            List<String> group = groupOf(token);
            if (group == null) {
                if (!expanded.contains(token)) {
                    expanded.add(token);
                }
            } else {
                for (String member : group) {
                    if (!expanded.contains(member)) {
                        expanded.add(member);
                    }
                }
            }
        }
        return expanded;
    }
}
