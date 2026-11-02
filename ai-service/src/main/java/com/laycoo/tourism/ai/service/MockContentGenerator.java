package com.laycoo.tourism.ai.service;

import com.laycoo.tourism.ai.dto.AiContent;
import com.laycoo.tourism.ai.dto.GeneratedResult;
import com.laycoo.tourism.ai.entity.AiTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 离线内置生成器：未配置大模型 API Key 时使用，保证演示环境也能跑通"生成 → 审核 → 入库"流程。
 * 输出结构与真实大模型一致，接入大模型后可直接切换。
 */
@Slf4j
@Component
public class MockContentGenerator implements ContentGenerator {

    private record Seed(String title, String blurb, String tag) {
    }

    private static final Map<String, List<Seed>> SEEDS = Map.ofEntries(
            Map.entry("FOOD", List.of(
                    new Seed("巷口那碗片儿川", "雪菜、笋片与瘦肉片在骨汤里翻滚，面条带着一点碱香，是本地人早起的理由。", "杭帮菜"),
                    new Seed("茶山脚下的农家菜", "院子里晒着笋干和梅干菜，灶台上的土鸡煲要炖两个小时，菜单跟着季节走。", "农家菜"))),
            Map.entry("STAY", List.of(
                    new Seed("西湖边的白墙小院", "推门是石板天井，二楼房间的窗正对着梧桐树冠，夜里能听见风吹叶子的声音。", "民宿"),
                    new Seed("运河畔的旧仓库民宿", "挑高六米的红砖空间保留了老仓库的钢梁，早餐在临水的露台上吃。", "设计酒店"))),
            Map.entry("PLAY", List.of(
                    new Seed("老厂房里的夜场演出", "红砖、钢梁和舞台灯光，乐队试音时整条巷子都在共振，散场后可以走到河边。", "演出"),
                    new Seed("西溪芦苇荡的清晨", "摇橹船从码头出发，船工不说话，只有水声和远处的水鸟，适合把手机收起来。", "自然体验"))),
            Map.entry("SHOP", List.of(
                    new Seed("中山中路的国货小店", "木柜台后摆着本地品牌的香皂、扇子和茶器，店主愿意讲每件东西的来历。", "国货"),
                    new Seed("巷子里的旧书与旧物", "旧书按年份码在墙上，角落里堆着老式相机和搪瓷杯，翻一下午也不会有人催。", "旧物"))),
            Map.entry("SPOT", List.of(
                    new Seed("半山看城的观景台", "傍晚爬上观景台，整座城的灯一盏盏亮起来，风里有桂花和江水的气味。", "城市景观"),
                    new Seed("钱塘江边的落日跑道", "江风把晚霞吹成一条橙线，跑步的人从身边经过，堤岸上的芦苇一直在响。", "落日"))),
            Map.entry("HERITAGE", List.of(
                    new Seed("运河边的老码头", "青石台阶被船缆磨出凹槽，旁边的界碑还能辨认出旧时的商号。", "运河"),
                    new Seed("山里的摩崖题刻", "石壁上的字被苔藓覆盖，得凑近才看得清落款，是几百年间路人留下的痕迹。", "石刻"))),
            Map.entry("INTANGIBLE", List.of(
                    new Seed("一把竹骨扇的诞生", "削竹、穿线、糊面、修边，老师傅说一把扇子要经过几十道手，急不得。", "手工艺"),
                    new Seed("巷子深处的木版年画", "梨木版上刻着门神，墨要刷得均匀，纸压上去的一瞬间不能移位。", "非遗"))),
            Map.entry("FOLK", List.of(
                    new Seed("立夏时的乌米饭", "南烛叶汁把糯米染成乌黑，蒸熟后带着草木清香，老人说吃了整个夏天不疰夏。", "节令"),
                    new Seed("老街上的糖画摊", "一勺麦芽糖在石板上走线，龙、凤、兔子在几十秒里成形，孩子们围着不肯走。", "市井"))),
            Map.entry("FIGURE", List.of(
                    new Seed("在故居里读一首旧诗", "老宅的窗棂把光切成格子，展柜里的手稿上有修改的墨迹，读一遍再抬头看院子。", "历史人物"),
                    new Seed("寻访吴越国的遗迹", "钱王祠的碑刻记着筑塘、疏湖的旧事，走完一圈再去看西湖，会多一层理解。", "吴越文化"))));

    private static final Map<String, String> CATEGORY_NAMES = Map.of(
            "FOOD", "餐饮", "STAY", "住宿", "PLAY", "休闲娱乐", "SHOP", "购物",
            "SPOT", "景点", "HERITAGE", "古迹", "INTANGIBLE", "非遗手作",
            "FOLK", "民俗", "FIGURE", "人文历史");

    private static final Map<String, String> HOURS = Map.of(
            "FOOD", "11:00-21:00", "STAY", "全天", "PLAY", "10:00-22:00", "SHOP", "10:00-21:00",
            "SPOT", "全天", "HERITAGE", "09:00-17:00", "INTANGIBLE", "10:00-17:00",
            "FOLK", "09:00-20:00", "FIGURE", "09:00-16:30");

    private static final Map<String, String> PRICES = Map.of(
            "FOOD", "人均 ¥60-120", "STAY", "¥380-780/晚", "PLAY", "¥80-180/人", "SHOP", "丰俭由人",
            "SPOT", "免费", "HERITAGE", "¥30-60", "INTANGIBLE", "体验 ¥120 起",
            "FOLK", "免费", "FIGURE", "免费");

    @Override
    public GeneratedResult generate(AiTask task) {
        String categoryCode = StringUtils.hasText(task.getCategoryCode()) ? task.getCategoryCode() : "SPOT";
        List<Seed> seeds = SEEDS.getOrDefault(categoryCode, SEEDS.get("SPOT"));
        String area = StringUtils.hasText(task.getArea()) ? task.getArea().trim() : "杭州";
        String categoryName = CATEGORY_NAMES.getOrDefault(categoryCode, "本地");
        int count = task.getTargetCount() == null ? 2 : Math.max(1, Math.min(task.getTargetCount(), 5));
        String keywords = StringUtils.hasText(task.getKeywords()) ? task.getKeywords().trim() : "";

        List<AiContent> contents = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Seed seed = seeds.get(i % seeds.size());
            Random random = new Random((task.getId() == null ? 7 : task.getId()) * 31L + i);
            String title = seed.title() + "·" + area + (i >= seeds.size() ? "（" + (i + 1) + "）" : "");
            String address = area + "·" + pick(random, List.of("南山路", "中山中路", "小河直街", "梅灵南路", "大井巷"))
                    + (10 + random.nextInt(80)) + "号";
            String blurb = seed.blurb();

            String kwLine = keywords.isEmpty() ? ""
                    : "如果正好对「" + keywords + "」感兴趣，这里会让这一趟更具体。";

            String body = "## 为什么值得去\n在" + area + "，" + seed.title() + "并不算最显眼的一处，却是本地人会在周末想起的地方。"
                    + blurb + "它适合把节奏放慢，用半天时间慢慢体会。\n\n"
                    + "## 特色体验\n" + categoryName + "的味道不只在招牌上，更在细节里："
                    + "进门先看一圈，再和店主聊两句，往往能听到菜单和简介里没有的故事。" + kwLine + "\n\n"
                    + "## 实用信息\n地址在" + address + "，营业时间" + HOURS.getOrDefault(categoryCode, "10:00-20:00")
                    + "，参考价格" + PRICES.getOrDefault(categoryCode, "丰俭由人") + "。建议错峰前往，留出足够的时间。";

            AiContent content = new AiContent();
            content.setTitle(title);
            content.setSummary(seed.title() + "：位于" + area + "的" + categoryName + "去处，" + blurb);
            content.setCategoryCode(categoryCode);
            content.setArea(area);
            content.setAddress(address);
            content.setBody(body);
            content.setHighlights(List.of(blurb.length() > 30 ? blurb.substring(0, 30) + "…" : blurb,
                    "本地人常去的" + categoryName + "去处", "适合半日慢游"));
            content.setTips("建议错开周末午后高峰；部分时段需要现场排队，出发前可先电话确认。");
            content.setOpeningHours(HOURS.getOrDefault(categoryCode, "10:00-20:00"));
            content.setPriceRange(PRICES.getOrDefault(categoryCode, "丰俭由人"));
            content.setCoverDesc(area + "的" + categoryName + "日常");
            content.setTags(new ArrayList<>(List.of(seed.tag(), area, categoryName, "本地推荐")));
            content.setAiTaskId(task.getId());
            contents.add(content);
        }
        log.info("内置生成器生成 {} 条内容，分类={}, 区域={}", contents.size(), categoryCode, area);
        return new GeneratedResult(contents, 0L, "mock-tourism-v1", "内置模板生成（未配置 AI_API_KEY）");
    }

    private String pick(Random random, List<String> values) {
        return values.get(random.nextInt(values.size()));
    }
}
