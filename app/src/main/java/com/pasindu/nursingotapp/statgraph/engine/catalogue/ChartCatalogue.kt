package com.pasindu.nursingotapp.statgraph.engine.catalogue

import com.pasindu.nursingotapp.statgraph.engine.recommendation.AnalyticalTask

/**
 * Authoritative pure-Kotlin chart catalogue for Stat Graph Studio.
 *
 * Covers Comparison, Trends, Distribution, Relationship, Composition, and
 * Statistical & Specialised chart specifications for Phase 2 recommendation
 * and validation (without claiming Phase 3 rendering support yet).
 */
object ChartCatalogue {

    val allSpecifications: List<ChartSpecification> = listOf(
        // --- 1. COMPARISON ---
        ChartSpecification(
            id = "bar_chart",
            canonicalName = "Bar chart",
            aliases = listOf("bar graph", "column chart", "vertical bar", "bar plot"),
            family = ChartFamily.COMPARISON,
            primaryTask = AnalyticalTask.COMPARE_CATEGORIES,
            supportedTasks = setOf(
                AnalyticalTask.COMPARE_CATEGORIES,
                AnalyticalTask.SHOW_RANKING,
                AnalyticalTask.COMPARE_GROUPS
            ),
            requiredRoles = setOf(
                RoleRequirement.CATEGORY_OR_TIME_DOMAIN,
                RoleRequirement.NUMERIC_MEASURE_OR_RATE
            ),
            optionalRoles = setOf(OptionalRoleSupport.GROUP_SERIES, OptionalRoleSupport.ESTIMATE_LIMITS),
            minObservations = 2,
            maxRecommendedCategories = 10,
            supportedCharacteristics = listOf("Discrete categories", "Aggregate counts", "Non-negative or signed bars"),
            knownLimitations = listOf("Long category labels collide on narrow phone widths", "Not ideal for >10 categories"),
            perceptualAccuracyRating = 90,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.GOOD,
                score = 80,
                notes = "Clear up to ~8–10 vertical bars; switch to horizontal bar for longer names."
            ),
            misleadingDesignRisks = listOf("Truncating the zero baseline exaggerates small differences"),
            howToRead = "Each vertical bar shows the number or rate for one group or time period.",
            plainWhy = "Useful when you want to see which ward, group, or period has the highest or lowest value."
        ),
        ChartSpecification(
            id = "horizontal_bar_chart",
            canonicalName = "Horizontal bar chart",
            aliases = listOf("horizontal bar", "barh", "ranked bar", "tornado bar"),
            family = ChartFamily.COMPARISON,
            primaryTask = AnalyticalTask.SHOW_RANKING,
            supportedTasks = setOf(
                AnalyticalTask.COMPARE_CATEGORIES,
                AnalyticalTask.SHOW_RANKING
            ),
            requiredRoles = setOf(
                RoleRequirement.CATEGORY_OR_TIME_DOMAIN,
                RoleRequirement.NUMERIC_MEASURE_OR_RATE
            ),
            optionalRoles = setOf(OptionalRoleSupport.LABEL),
            minObservations = 2,
            maxRecommendedCategories = 25,
            supportedCharacteristics = listOf("Long ward/diagnosis labels", "Sorted rankings", "Many categories"),
            knownLimitations = listOf("Less natural for sequential time trends than line charts"),
            perceptualAccuracyRating = 92,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.EXCELLENT,
                score = 94,
                notes = "Vertical scrolling accommodates long clinical labels cleanly on portrait phones."
            ),
            misleadingDesignRisks = listOf("Non-zero baseline distorts bar length comparisons")
        ),
        ChartSpecification(
            id = "grouped_bar_chart",
            canonicalName = "Grouped bar chart",
            aliases = listOf("clustered bar", "clustered column", "side by side bar", "multi bar"),
            family = ChartFamily.COMPARISON,
            primaryTask = AnalyticalTask.COMPARE_GROUPS,
            supportedTasks = setOf(
                AnalyticalTask.COMPARE_GROUPS,
                AnalyticalTask.COMPARE_CATEGORIES
            ),
            requiredRoles = setOf(
                RoleRequirement.CATEGORY_OR_TIME_DOMAIN,
                RoleRequirement.NUMERIC_MEASURE_OR_RATE,
                RoleRequirement.MULTIPLE_NUMERIC_OR_GROUP
            ),
            optionalRoles = setOf(OptionalRoleSupport.GROUP_SERIES),
            minObservations = 2,
            maxRecommendedCategories = 8,
            supportedCharacteristics = listOf("Comparing Total vs Part counts side-by-side", "2–4 series per category/period"),
            knownLimitations = listOf("Becomes crowded with >4 series or >8 categories on mobile screens"),
            perceptualAccuracyRating = 86,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.GOOD,
                score = 78,
                notes = "Effective for 2–3 metrics across 3–8 periods or categories."
            ),
            misleadingDesignRisks = listOf("Comparing disparate units on a shared axis misleads readers"),
            howToRead = "Bars are placed side by side so you can compare two or more values within each group or period.",
            plainWhy = "Useful for comparing different groups or measures side by side across the same categories."
        ),
        ChartSpecification(
            id = "stacked_bar_chart",
            canonicalName = "Stacked bar chart",
            aliases = listOf("stacked column", "cumulative bar"),
            family = ChartFamily.COMPARISON,
            primaryTask = AnalyticalTask.SHOW_COMPOSITION,
            supportedTasks = setOf(
                AnalyticalTask.SHOW_COMPOSITION,
                AnalyticalTask.COMPARE_CATEGORIES,
                AnalyticalTask.COMPARE_GROUPS
            ),
            requiredRoles = setOf(
                RoleRequirement.CATEGORY_OR_TIME_DOMAIN,
                RoleRequirement.NUMERIC_MEASURE_OR_RATE,
                RoleRequirement.MULTIPLE_NUMERIC_OR_GROUP
            ),
            optionalRoles = setOf(OptionalRoleSupport.GROUP_SERIES),
            minObservations = 2,
            maxRecommendedCategories = 10,
            supportedCharacteristics = listOf("Non-negative additive components", "Total + subgroup breakdown"),
            knownLimitations = listOf("Upper stacked segments lack a common baseline for precise comparison"),
            perceptualAccuracyRating = 72,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.GOOD,
                score = 76,
                notes = "Readable up to 4–5 segments; avoid stacking unrelated totals and subsets together."
            ),
            misleadingDesignRisks = listOf(
                "Stacking a subset count on top of its own total double-counts the total",
                "Stacking negative and positive values breaks baseline meaning"
            ),
            requiresNonNegativeValues = true,
            howToRead = "Each bar is split into coloured parts that add up to a combined total for that group or period.",
            plainWhy = "Useful when you want to see both the overall total and how much each part contributes."
        ),
        ChartSpecification(
            id = "dot_plot",
            canonicalName = "Dot plot",
            aliases = listOf("cleveland dot plot", "dot chart"),
            family = ChartFamily.COMPARISON,
            primaryTask = AnalyticalTask.COMPARE_CATEGORIES,
            supportedTasks = setOf(
                AnalyticalTask.COMPARE_CATEGORIES,
                AnalyticalTask.SHOW_RANKING,
                AnalyticalTask.SHOW_RATES
            ),
            requiredRoles = setOf(
                RoleRequirement.CATEGORY_OR_TIME_DOMAIN,
                RoleRequirement.NUMERIC_MEASURE_OR_RATE
            ),
            optionalRoles = setOf(OptionalRoleSupport.GROUP_SERIES),
            minObservations = 2,
            maxRecommendedCategories = 25,
            supportedCharacteristics = listOf("Rates or indices not starting at zero", "High-precision position encoding"),
            knownLimitations = listOf("Less familiar to general audiences than bar charts"),
            perceptualAccuracyRating = 96,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.EXCELLENT,
                score = 90,
                notes = "Low ink density keeps dense categorical comparisons crisp on phones."
            ),
            misleadingDesignRisks = listOf("Missing gridlines make exact value lookup harder")
        ),
        ChartSpecification(
            id = "lollipop_chart",
            canonicalName = "Lollipop chart",
            aliases = listOf("lollipop plot", "stem plot"),
            family = ChartFamily.COMPARISON,
            primaryTask = AnalyticalTask.SHOW_RANKING,
            supportedTasks = setOf(
                AnalyticalTask.SHOW_RANKING,
                AnalyticalTask.COMPARE_CATEGORIES
            ),
            requiredRoles = setOf(
                RoleRequirement.CATEGORY_OR_TIME_DOMAIN,
                RoleRequirement.NUMERIC_MEASURE_OR_RATE
            ),
            optionalRoles = setOf(OptionalRoleSupport.LABEL),
            minObservations = 2,
            maxRecommendedCategories = 20,
            supportedCharacteristics = listOf("Ranked categories", "Sparse clutter-free magnitude display"),
            knownLimitations = listOf("Circle center can slightly obscure exact end value vs flat bar edge"),
            perceptualAccuracyRating = 88,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.EXCELLENT,
                score = 89,
                notes = "Clean stems reduce visual noise on smaller screens."
            ),
            misleadingDesignRisks = listOf("Non-zero baseline creates misleading stem lengths")
        ),

        // --- 2. TRENDS ---
        ChartSpecification(
            id = "line_chart",
            canonicalName = "Line chart",
            aliases = listOf("trend graph", "trend chart", "line graph", "time series", "time series chart"),
            family = ChartFamily.TRENDS,
            primaryTask = AnalyticalTask.SHOW_CHANGE_OVER_TIME,
            supportedTasks = setOf(
                AnalyticalTask.SHOW_CHANGE_OVER_TIME,
                AnalyticalTask.SHOW_RATES,
                AnalyticalTask.SHOW_CUMULATIVE_TOTALS,
                AnalyticalTask.COMPARE_PROPORTIONS
            ),
            requiredRoles = setOf(
                RoleRequirement.TIME_DOMAIN,
                RoleRequirement.NUMERIC_MEASURE_OR_RATE
            ),
            optionalRoles = setOf(OptionalRoleSupport.DERIVED_RATE, OptionalRoleSupport.ESTIMATE_LIMITS),
            minObservations = 3,
            supportedCharacteristics = listOf("Ordered temporal axis", "Continuous or aggregate period counts", "Rate/percentage trends"),
            knownLimitations = listOf("Connecting across large missing time gaps can imply unmeasured continuity"),
            perceptualAccuracyRating = 94,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.EXCELLENT,
                score = 92,
                notes = "Top performer on mobile portrait/landscape viewports for temporal trends."
            ),
            misleadingDesignRisks = listOf(
                "Using on unordered nominal categories falsely implies continuity",
                "Interpolating across missing periods masks gaps"
            ),
            howToRead = "Points are joined from left to right in time order to show how a single value moves over time.",
            plainWhy = "Useful for spotting rises, drops, or steady periods across days, weeks, or months."
        ),
        ChartSpecification(
            id = "multi_series_line_chart",
            canonicalName = "Multi-series line chart",
            aliases = listOf("multi line", "multiple line chart", "multi-line chart", "grouped line chart"),
            family = ChartFamily.TRENDS,
            primaryTask = AnalyticalTask.SHOW_CHANGE_OVER_TIME,
            supportedTasks = setOf(
                AnalyticalTask.SHOW_CHANGE_OVER_TIME,
                AnalyticalTask.COMPARE_GROUPS,
                AnalyticalTask.SHOW_RATES
            ),
            requiredRoles = setOf(
                RoleRequirement.TIME_DOMAIN,
                RoleRequirement.NUMERIC_MEASURE_OR_RATE,
                RoleRequirement.MULTIPLE_NUMERIC_OR_GROUP
            ),
            optionalRoles = setOf(OptionalRoleSupport.GROUP_SERIES),
            minObservations = 3,
            supportedCharacteristics = listOf("Multiple time-aligned measures or stratified cohorts"),
            knownLimitations = listOf("Spaghetti clutter if >5 series overlap", "Incompatible scales require separate panels or normalization"),
            perceptualAccuracyRating = 88,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.GOOD,
                score = 80,
                notes = "Clear with 2–4 well-contrasted series and direct labels."
            ),
            misleadingDesignRisks = listOf("Dual y-axes can fabricate false crossovers", "Scale differences between Total and small Subset can flatten the subset"),
            howToRead = "Each coloured line follows one group or measure over time on the same scale.",
            plainWhy = "Useful for comparing whether two or more wards or measures rise and fall in a similar pattern."
        ),
        ChartSpecification(
            id = "area_chart",
            canonicalName = "Area chart",
            aliases = listOf("filled line chart", "area graph"),
            family = ChartFamily.TRENDS,
            primaryTask = AnalyticalTask.SHOW_CHANGE_OVER_TIME,
            supportedTasks = setOf(
                AnalyticalTask.SHOW_CHANGE_OVER_TIME,
                AnalyticalTask.SHOW_CUMULATIVE_TOTALS
            ),
            requiredRoles = setOf(
                RoleRequirement.TIME_DOMAIN,
                RoleRequirement.NUMERIC_MEASURE_OR_RATE
            ),
            optionalRoles = setOf(OptionalRoleSupport.DERIVED_RATE),
            minObservations = 3,
            supportedCharacteristics = listOf("Zero-anchored non-negative temporal volumes"),
            knownLimitations = listOf("Must start at zero; poor for rates bounded far above zero"),
            perceptualAccuracyRating = 80,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.GOOD,
                score = 82,
                notes = "Readable for single volume trends over time."
            ),
            misleadingDesignRisks = listOf("Truncated baseline distorts shaded area"),
            requiresNonNegativeValues = true
        ),
        ChartSpecification(
            id = "stacked_area_chart",
            canonicalName = "Stacked area chart",
            aliases = listOf("stacked area", "cumulative area"),
            family = ChartFamily.TRENDS,
            primaryTask = AnalyticalTask.SHOW_COMPOSITION,
            supportedTasks = setOf(
                AnalyticalTask.SHOW_COMPOSITION,
                AnalyticalTask.SHOW_CHANGE_OVER_TIME
            ),
            requiredRoles = setOf(
                RoleRequirement.TIME_DOMAIN,
                RoleRequirement.NUMERIC_MEASURE_OR_RATE,
                RoleRequirement.MULTIPLE_NUMERIC_OR_GROUP
            ),
            optionalRoles = setOf(OptionalRoleSupport.GROUP_SERIES),
            minObservations = 3,
            supportedCharacteristics = listOf("Part-to-whole volumes evolving over ordered time"),
            knownLimitations = listOf("Upper layers suffer from baseline shift illusion"),
            perceptualAccuracyRating = 66,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.MODERATE,
                score = 64,
                notes = "Limit to 2–4 additive strata on phone screens."
            ),
            misleadingDesignRisks = listOf("Stacking non-additive metrics or Total + Subset creates invalid totals"),
            requiresNonNegativeValues = true
        ),
        ChartSpecification(
            id = "step_chart",
            canonicalName = "Step chart",
            aliases = listOf("step line", "stepped line chart", "staircase chart"),
            family = ChartFamily.TRENDS,
            primaryTask = AnalyticalTask.SHOW_CHANGE_OVER_TIME,
            supportedTasks = setOf(
                AnalyticalTask.SHOW_CHANGE_OVER_TIME,
                AnalyticalTask.SHOW_CUMULATIVE_TOTALS
            ),
            requiredRoles = setOf(
                RoleRequirement.TIME_DOMAIN,
                RoleRequirement.NUMERIC_MEASURE_OR_RATE
            ),
            optionalRoles = setOf(OptionalRoleSupport.GROUP_SERIES),
            minObservations = 3,
            supportedCharacteristics = listOf("Discrete regime shifts, policy thresholds, or cumulative steps"),
            knownLimitations = listOf("Can obscure smooth intra-period interpolation"),
            perceptualAccuracyRating = 87,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.GOOD,
                score = 81,
                notes = "Crisp horizontal-vertical steps work well on mobile displays."
            ),
            misleadingDesignRisks = listOf("Implies constant hold between timestamps")
        ),

        // --- 3. DISTRIBUTION ---
        ChartSpecification(
            id = "histogram",
            canonicalName = "Histogram",
            aliases = listOf("frequency distribution", "frequency histogram", "bin chart"),
            family = ChartFamily.DISTRIBUTION,
            primaryTask = AnalyticalTask.SHOW_DISTRIBUTION,
            supportedTasks = setOf(AnalyticalTask.SHOW_DISTRIBUTION),
            requiredRoles = setOf(RoleRequirement.NUMERIC_MEASURE_OR_RATE),
            minObservations = 5,
            supportedCharacteristics = listOf("Continuous or wide-range count distribution", "Skewness and modality detection"),
            knownLimitations = listOf("Bin width strongly alters apparent shape", "Unreliable with <8–10 observations"),
            perceptualAccuracyRating = 84,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.GOOD,
                score = 78,
                notes = "Use 5–10 bins for readable phone layout."
            ),
            misleadingDesignRisks = listOf("Too many bins create noisy spikes on small samples; too few hide multimodality")
        ),
        ChartSpecification(
            id = "box_plot",
            canonicalName = "Box plot",
            aliases = listOf("boxplot", "box and whisker", "whisker plot", "iqr plot"),
            family = ChartFamily.DISTRIBUTION,
            primaryTask = AnalyticalTask.SHOW_DISTRIBUTION,
            supportedTasks = setOf(
                AnalyticalTask.SHOW_DISTRIBUTION,
                AnalyticalTask.COMPARE_GROUPS
            ),
            requiredRoles = setOf(RoleRequirement.NUMERIC_MEASURE_OR_RATE),
            optionalRoles = setOf(OptionalRoleSupport.CATEGORY, OptionalRoleSupport.GROUP_SERIES),
            minObservations = 5,
            supportedCharacteristics = listOf("Median, IQR, and extreme outliers", "Skewed numeric data"),
            knownLimitations = listOf("Hides bimodal distributions and exact sample size"),
            perceptualAccuracyRating = 86,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.GOOD,
                score = 82,
                notes = "Compact quartile summary scales cleanly across 1–6 groups."
            ),
            misleadingDesignRisks = listOf("Showing box plots for n < 5 exaggerates quartile precision")
        ),
        ChartSpecification(
            id = "violin_plot",
            canonicalName = "Violin plot",
            aliases = listOf("violin chart"),
            family = ChartFamily.DISTRIBUTION,
            primaryTask = AnalyticalTask.SHOW_DISTRIBUTION,
            supportedTasks = setOf(
                AnalyticalTask.SHOW_DISTRIBUTION,
                AnalyticalTask.COMPARE_GROUPS
            ),
            requiredRoles = setOf(RoleRequirement.NUMERIC_MEASURE_OR_RATE),
            optionalRoles = setOf(OptionalRoleSupport.CATEGORY, OptionalRoleSupport.GROUP_SERIES),
            minObservations = 10,
            supportedCharacteristics = listOf("Kernel density shape across groups", "Multimodal distributions"),
            knownLimitations = listOf("Requires larger sample sizes (n >= 10) to avoid synthetic smoothing artifacts"),
            perceptualAccuracyRating = 78,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.MODERATE,
                score = 66,
                notes = "Wide violins crowd narrow phones when >4 groups are displayed."
            ),
            misleadingDesignRisks = listOf("Smooth density tails can extend beyond impossible physical bounds (e.g., < 0)")
        ),
        ChartSpecification(
            id = "density_plot",
            canonicalName = "Density plot",
            aliases = listOf("kde plot", "kernel density", "smooth distribution"),
            family = ChartFamily.DISTRIBUTION,
            primaryTask = AnalyticalTask.SHOW_DISTRIBUTION,
            supportedTasks = setOf(AnalyticalTask.SHOW_DISTRIBUTION),
            requiredRoles = setOf(RoleRequirement.NUMERIC_MEASURE_OR_RATE),
            optionalRoles = setOf(OptionalRoleSupport.GROUP_SERIES),
            minObservations = 10,
            supportedCharacteristics = listOf("Smooth continuous distribution shape"),
            knownLimitations = listOf("Bandwidth choice sensitive; misleading on small n (<10)"),
            perceptualAccuracyRating = 79,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.GOOD,
                score = 76,
                notes = "Clean curve for 1–3 overlapping distributions."
            ),
            misleadingDesignRisks = listOf("Over-smoothing masks gaps and outliers")
        ),
        ChartSpecification(
            id = "strip_plot",
            canonicalName = "Strip plot",
            aliases = listOf("jitter plot", "beeswarm", "1d scatter"),
            family = ChartFamily.DISTRIBUTION,
            primaryTask = AnalyticalTask.SHOW_DISTRIBUTION,
            supportedTasks = setOf(
                AnalyticalTask.SHOW_DISTRIBUTION,
                AnalyticalTask.COMPARE_GROUPS
            ),
            requiredRoles = setOf(RoleRequirement.NUMERIC_MEASURE_OR_RATE),
            optionalRoles = setOf(OptionalRoleSupport.CATEGORY, OptionalRoleSupport.GROUP_SERIES),
            minObservations = 3,
            supportedCharacteristics = listOf("Small-to-medium datasets", "Individual observation transparency"),
            knownLimitations = listOf("Overplotting on large datasets without jitter"),
            perceptualAccuracyRating = 88,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.GOOD,
                score = 83,
                notes = "Ideal for small clinical samples (n = 3..30) without hiding raw points."
            ),
            misleadingDesignRisks = listOf("Overlapping points conceal duplicates unless jittered")
        ),

        // --- 4. RELATIONSHIP ---
        ChartSpecification(
            id = "scatter_plot",
            canonicalName = "Scatter plot",
            aliases = listOf("scatter graph", "scatter chart", "correlation graph", "xy plot", "correlation plot"),
            family = ChartFamily.RELATIONSHIP,
            primaryTask = AnalyticalTask.SHOW_RELATIONSHIP,
            supportedTasks = setOf(
                AnalyticalTask.SHOW_RELATIONSHIP,
                AnalyticalTask.SHOW_CORRELATION
            ),
            requiredRoles = setOf(RoleRequirement.TWO_NUMERIC_FIELDS),
            optionalRoles = setOf(OptionalRoleSupport.GROUP_SERIES, OptionalRoleSupport.LABEL),
            minObservations = 3,
            supportedCharacteristics = listOf("Paired numeric measurements", "Linearity, clusters, and bivariate outliers"),
            knownLimitations = listOf("Does not preserve sequential time ordering unless points are connected/annotated"),
            perceptualAccuracyRating = 93,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.GOOD,
                score = 82,
                notes = "Crisp X-Y point cloud on portrait or landscape phone layouts."
            ),
            misleadingDesignRisks = listOf("Correlation does not imply causation; sparse n < 5 can exaggerate r"),
            howToRead = "Each dot represents one row, placed by its horizontal value and vertical value.",
            plainWhy = "Useful for seeing whether two numbers tend to move together or cluster in patterns — though a pattern alone does not prove one causes the other."
        ),
        ChartSpecification(
            id = "bubble_chart",
            canonicalName = "Bubble chart",
            aliases = listOf("bubble plot", "weighted scatter"),
            family = ChartFamily.RELATIONSHIP,
            primaryTask = AnalyticalTask.SHOW_RELATIONSHIP,
            supportedTasks = setOf(
                AnalyticalTask.SHOW_RELATIONSHIP,
                AnalyticalTask.SHOW_CORRELATION
            ),
            requiredRoles = setOf(RoleRequirement.THREE_NUMERIC_FIELDS),
            optionalRoles = setOf(OptionalRoleSupport.GROUP_SERIES, OptionalRoleSupport.LABEL),
            minObservations = 3,
            supportedCharacteristics = listOf("3 numeric variables (X, Y, positive size)"),
            knownLimitations = listOf("Area judgment of circle size is perceptually imprecise"),
            perceptualAccuracyRating = 68,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.MODERATE,
                score = 60,
                notes = "Large bubbles overlap quickly on narrow mobile screens."
            ),
            misleadingDesignRisks = listOf("Scaling radius instead of area squares perceived magnitude"),
            requiresNonNegativeValues = true
        ),
        ChartSpecification(
            id = "correlation_heatmap",
            canonicalName = "Correlation heatmap",
            aliases = listOf("correlation matrix", "heatmap", "heat map"),
            family = ChartFamily.RELATIONSHIP,
            primaryTask = AnalyticalTask.SHOW_CORRELATION,
            supportedTasks = setOf(AnalyticalTask.SHOW_CORRELATION),
            requiredRoles = setOf(RoleRequirement.TWO_NUMERIC_FIELDS),
            minObservations = 3,
            supportedCharacteristics = listOf("Pairwise Pearson r matrix across numeric fields"),
            knownLimitations = listOf("Color-only encoding has lower precision than scatter plots", "Only captures linear correlation"),
            perceptualAccuracyRating = 64,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.MODERATE,
                score = 65,
                notes = "Readable up to 5×5 matrices on phone; annotate cells with numeric r."
            ),
            misleadingDesignRisks = listOf("Non-diverging color scales hide positive vs negative sign", "Small n produces extreme ±1 correlations")
        ),

        // --- 5. COMPOSITION ---
        ChartSpecification(
            id = "pie_chart",
            canonicalName = "Pie chart",
            aliases = listOf("circle chart", "pie graph", "sector chart"),
            family = ChartFamily.COMPOSITION,
            primaryTask = AnalyticalTask.SHOW_COMPOSITION,
            supportedTasks = setOf(AnalyticalTask.SHOW_COMPOSITION),
            requiredRoles = setOf(
                RoleRequirement.CATEGORY_DOMAIN,
                RoleRequirement.NUMERIC_MEASURE_OR_RATE
            ),
            minObservations = 2,
            maxRecommendedCategories = 5,
            supportedCharacteristics = listOf("Single snapshot part-to-whole across 2–5 categories"),
            knownLimitations = listOf(
                "Poor angular comparison across similar slice sizes",
                "Unsuitable for sequential time trends or negative values"
            ),
            perceptualAccuracyRating = 50,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.MODERATE,
                score = 64,
                notes = "Readable only with <= 4–5 slices and direct percentage labels."
            ),
            misleadingDesignRisks = listOf(
                "Using pie charts across time periods obscures trends",
                "Slices that do not sum to a meaningful whole misrepresent data"
            ),
            requiresNonNegativeValues = true,
            requiresPartToWhole = true,
            disallowsSequentialTimeComparison = true
        ),
        ChartSpecification(
            id = "donut_chart",
            canonicalName = "Donut chart",
            aliases = listOf("doughnut chart", "ring chart"),
            family = ChartFamily.COMPOSITION,
            primaryTask = AnalyticalTask.SHOW_COMPOSITION,
            supportedTasks = setOf(AnalyticalTask.SHOW_COMPOSITION),
            requiredRoles = setOf(
                RoleRequirement.CATEGORY_DOMAIN,
                RoleRequirement.NUMERIC_MEASURE_OR_RATE
            ),
            minObservations = 2,
            maxRecommendedCategories = 5,
            supportedCharacteristics = listOf("Single KPI proportion or 2–5 part-to-whole categories"),
            knownLimitations = listOf("Arc-length judgment is weaker than common-baseline bar position"),
            perceptualAccuracyRating = 52,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.MODERATE,
                score = 68,
                notes = "Center KPI callout helps when slices <= 5."
            ),
            misleadingDesignRisks = listOf("Comparing multiple time periods via multiple donuts hinders comparison"),
            requiresNonNegativeValues = true,
            requiresPartToWhole = true,
            disallowsSequentialTimeComparison = true
        ),
        ChartSpecification(
            id = "stacked_bar_100_chart",
            canonicalName = "100% stacked bar chart",
            aliases = listOf("percentage comparison", "100% stacked", "normalized stacked bar", "proportion stacked bar"),
            family = ChartFamily.COMPOSITION,
            primaryTask = AnalyticalTask.COMPARE_PROPORTIONS,
            supportedTasks = setOf(
                AnalyticalTask.COMPARE_PROPORTIONS,
                AnalyticalTask.SHOW_COMPOSITION,
                AnalyticalTask.SHOW_RATES
            ),
            requiredRoles = setOf(
                RoleRequirement.CATEGORY_OR_TIME_DOMAIN,
                RoleRequirement.NUMERIC_MEASURE_OR_RATE,
                RoleRequirement.PART_TO_WHOLE_STRUCTURE
            ),
            optionalRoles = setOf(OptionalRoleSupport.GROUP_SERIES, OptionalRoleSupport.DERIVED_RATE),
            minObservations = 2,
            maxRecommendedCategories = 10,
            supportedCharacteristics = listOf("Normalized 0–100% part-to-whole across periods or categories"),
            knownLimitations = listOf("Hides differences in absolute total denominator size unless annotated"),
            perceptualAccuracyRating = 80,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.GOOD,
                score = 81,
                notes = "Far cleaner than multiple pie charts for comparing proportions across months or wards."
            ),
            misleadingDesignRisks = listOf("Omitting denominator counts hides low-sample periods"),
            requiresNonNegativeValues = true,
            requiresPartToWhole = true,
            howToRead = "Every bar reaches 100%, showing the percentage share of each part out of that row's total.",
            plainWhy = "Useful for comparing proportions fairly across wards or months even when total patient numbers differ."
        ),
        ChartSpecification(
            id = "treemap",
            canonicalName = "Treemap",
            aliases = listOf("tree map", "mosaic tiles"),
            family = ChartFamily.COMPOSITION,
            primaryTask = AnalyticalTask.SHOW_COMPOSITION,
            supportedTasks = setOf(AnalyticalTask.SHOW_COMPOSITION),
            requiredRoles = setOf(
                RoleRequirement.CATEGORY_DOMAIN,
                RoleRequirement.NUMERIC_MEASURE_OR_RATE
            ),
            optionalRoles = setOf(OptionalRoleSupport.GROUP_SERIES),
            minObservations = 3,
            maxRecommendedCategories = 20,
            supportedCharacteristics = listOf("Hierarchical or many-category non-negative totals"),
            knownLimitations = listOf("Area comparison is less accurate than bar length"),
            perceptualAccuracyRating = 58,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.MODERATE,
                score = 60,
                notes = "Small rectangles drop labels on narrow phones."
            ),
            misleadingDesignRisks = listOf("Cannot represent zero or negative values"),
            requiresNonNegativeValues = true,
            requiresPartToWhole = true,
            disallowsSequentialTimeComparison = true
        ),

        // --- 6. STATISTICAL & SPECIALISED ---
        ChartSpecification(
            id = "error_bar_chart",
            canonicalName = "Error-bar chart",
            aliases = listOf("error bar", "confidence interval chart", "ci bar", "mean with error bars"),
            family = ChartFamily.STATISTICAL_SPECIALISED,
            primaryTask = AnalyticalTask.SHOW_UNCERTAINTY,
            supportedTasks = setOf(
                AnalyticalTask.SHOW_UNCERTAINTY,
                AnalyticalTask.COMPARE_CATEGORIES,
                AnalyticalTask.SHOW_RATES
            ),
            requiredRoles = setOf(
                RoleRequirement.CATEGORY_OR_TIME_DOMAIN,
                RoleRequirement.ESTIMATE_WITH_LIMITS
            ),
            minObservations = 1,
            supportedCharacteristics = listOf("Point estimates with symmetric/asymmetric lower and upper limits"),
            knownLimitations = listOf("Requires explicit lower and upper uncertainty limits"),
            perceptualAccuracyRating = 88,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.GOOD,
                score = 82,
                notes = "Clean point + whisker markers avoid within-bar visual bias."
            ),
            misleadingDesignRisks = listOf("Bar-plus-error-bar hides asymmetric intervals if baseline is distorted")
        ),
        ChartSpecification(
            id = "range_plot",
            canonicalName = "Range plot",
            aliases = listOf("interval plot", "min max plot", "high low plot"),
            family = ChartFamily.STATISTICAL_SPECIALISED,
            primaryTask = AnalyticalTask.SHOW_UNCERTAINTY,
            supportedTasks = setOf(
                AnalyticalTask.SHOW_UNCERTAINTY,
                AnalyticalTask.COMPARE_CATEGORIES
            ),
            requiredRoles = setOf(
                RoleRequirement.CATEGORY_OR_TIME_DOMAIN,
                RoleRequirement.ESTIMATE_WITH_LIMITS
            ),
            minObservations = 2,
            supportedCharacteristics = listOf("Interval spans (lower to upper limit) per category or period"),
            knownLimitations = listOf("Requires explicit lower and upper bounds"),
            perceptualAccuracyRating = 90,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.EXCELLENT,
                score = 87,
                notes = "Horizontal interval spans remain clear even on narrow portrait phones."
            ),
            misleadingDesignRisks = listOf("Inverted bounds (lower > upper) corrupt span direction")
        ),
        ChartSpecification(
            id = "forest_plot",
            canonicalName = "Forest plot",
            aliases = listOf("blobbogram", "odds ratio plot", "meta analysis plot"),
            family = ChartFamily.STATISTICAL_SPECIALISED,
            primaryTask = AnalyticalTask.SHOW_UNCERTAINTY,
            supportedTasks = setOf(
                AnalyticalTask.SHOW_UNCERTAINTY,
                AnalyticalTask.COMPARE_GROUPS
            ),
            requiredRoles = setOf(
                RoleRequirement.CATEGORY_OR_TIME_DOMAIN,
                RoleRequirement.ESTIMATE_WITH_LIMITS
            ),
            minObservations = 2,
            supportedCharacteristics = listOf("Point estimates + 95% confidence intervals against a null reference line"),
            knownLimitations = listOf("Requires explicit point estimate and lower/upper CI limits"),
            perceptualAccuracyRating = 93,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.GOOD,
                score = 77,
                notes = "Works well horizontally; compact table column requires concise labels."
            ),
            misleadingDesignRisks = listOf("Using linear scale instead of log scale for odds/hazard ratios distorts symmetry")
        ),
        ChartSpecification(
            id = "waterfall_chart",
            canonicalName = "Waterfall chart",
            aliases = listOf("bridge chart", "stepped bridge"),
            family = ChartFamily.STATISTICAL_SPECIALISED,
            primaryTask = AnalyticalTask.SHOW_CUMULATIVE_TOTALS,
            supportedTasks = setOf(
                AnalyticalTask.SHOW_CUMULATIVE_TOTALS,
                AnalyticalTask.COMPARE_CATEGORIES
            ),
            requiredRoles = setOf(
                RoleRequirement.CATEGORY_OR_TIME_DOMAIN,
                RoleRequirement.NUMERIC_MEASURE_OR_RATE
            ),
            minObservations = 3,
            maxRecommendedCategories = 12,
            supportedCharacteristics = listOf("Sequential positive/negative increments building a cumulative total"),
            knownLimitations = listOf("Confusing when increments are not additive deltas"),
            perceptualAccuracyRating = 79,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.MODERATE,
                score = 68,
                notes = "Clear up to 8–10 sequential steps."
            ),
            misleadingDesignRisks = listOf("Applying to independent totals instead of deltas produces nonsensical floating bars")
        ),
        ChartSpecification(
            id = "funnel_chart",
            canonicalName = "Funnel chart",
            aliases = listOf("stage funnel", "cascade chart", "screening funnel"),
            family = ChartFamily.STATISTICAL_SPECIALISED,
            primaryTask = AnalyticalTask.COMPARE_PROPORTIONS,
            supportedTasks = setOf(
                AnalyticalTask.COMPARE_PROPORTIONS,
                AnalyticalTask.SHOW_RANKING
            ),
            requiredRoles = setOf(
                RoleRequirement.CATEGORY_DOMAIN,
                RoleRequirement.NUMERIC_MEASURE_OR_RATE
            ),
            minObservations = 3,
            maxRecommendedCategories = 8,
            supportedCharacteristics = listOf("Sequential monotonically decreasing care-cascade stages"),
            knownLimitations = listOf("Misleading if stage counts increase instead of narrowing"),
            perceptualAccuracyRating = 65,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.GOOD,
                score = 76,
                notes = "Vertical cascade fits portrait phone aspect ratios naturally."
            ),
            misleadingDesignRisks = listOf("Centered trapezoids hinder length comparison compared to horizontal bars"),
            requiresNonNegativeValues = true
        ),
        ChartSpecification(
            id = "population_pyramid",
            canonicalName = "Population pyramid",
            aliases = listOf("age sex pyramid", "demographic pyramid", "mirrored bar"),
            family = ChartFamily.STATISTICAL_SPECIALISED,
            primaryTask = AnalyticalTask.COMPARE_GROUPS,
            supportedTasks = setOf(
                AnalyticalTask.COMPARE_GROUPS,
                AnalyticalTask.SHOW_DISTRIBUTION
            ),
            requiredRoles = setOf(
                RoleRequirement.CATEGORY_DOMAIN,
                RoleRequirement.NUMERIC_MEASURE_OR_RATE,
                RoleRequirement.MULTIPLE_NUMERIC_OR_GROUP
            ),
            minObservations = 3,
            supportedCharacteristics = listOf("Ordered age-band categories split across two opposing groups (e.g., Sex)"),
            knownLimitations = listOf("Requires exactly 2 opposing groups and ordered strata"),
            perceptualAccuracyRating = 82,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.GOOD,
                score = 79,
                notes = "Mirrored horizontal bars align neatly on portrait phones."
            ),
            misleadingDesignRisks = listOf("Unequal age-band widths distort demographic proportions"),
            requiresNonNegativeValues = true
        ),
        ChartSpecification(
            id = "control_chart",
            canonicalName = "Control chart",
            aliases = listOf("shewhart chart", "spc chart", "run chart", "process control"),
            family = ChartFamily.STATISTICAL_SPECIALISED,
            primaryTask = AnalyticalTask.SHOW_CHANGE_OVER_TIME,
            supportedTasks = setOf(
                AnalyticalTask.SHOW_CHANGE_OVER_TIME,
                AnalyticalTask.SHOW_RATES,
                AnalyticalTask.SHOW_UNCERTAINTY
            ),
            requiredRoles = setOf(
                RoleRequirement.TIME_DOMAIN,
                RoleRequirement.NUMERIC_MEASURE_OR_RATE
            ),
            minObservations = 6,
            supportedCharacteristics = listOf("Longitudinal quality/infection monitoring with mean & UCL/LCL bands"),
            knownLimitations = listOf("Control limits require >= 6–12 baseline time points to avoid false alarms"),
            perceptualAccuracyRating = 92,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.GOOD,
                score = 81,
                notes = "Time trend with clear horizontal control bands reads well on mobile."
            ),
            misleadingDesignRisks = listOf("Computing control limits from <6 observations creates unstable bands")
        ),
        ChartSpecification(
            id = "kaplan_meier_curve",
            canonicalName = "Kaplan–Meier-style survival curve",
            aliases = listOf("kaplan meier", "kaplan-meier", "km curve", "survival curve", "survival plot", "time to event"),
            family = ChartFamily.STATISTICAL_SPECIALISED,
            primaryTask = AnalyticalTask.SHOW_SURVIVAL_OVER_TIME,
            supportedTasks = setOf(
                AnalyticalTask.SHOW_SURVIVAL_OVER_TIME,
                AnalyticalTask.COMPARE_GROUPS
            ),
            requiredRoles = setOf(RoleRequirement.DURATION_WITH_EVENT),
            optionalRoles = setOf(OptionalRoleSupport.GROUP_SERIES),
            minObservations = 5,
            supportedCharacteristics = listOf("Time-to-event duration paired with explicit event/censoring flag"),
            knownLimitations = listOf("Impossible without both duration and event/censoring status"),
            perceptualAccuracyRating = 91,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.GOOD,
                score = 78,
                notes = "Stepwise survival drop with censoring ticks fits landscape/portrait views."
            ),
            misleadingDesignRisks = listOf("Omitting censoring markers or numbers-at-risk obscures late-tail uncertainty"),
            requiresNonNegativeValues = true
        ),

        // --- 7. BATCH 1A FOUNDATIONAL CORE CHARTS ---
        ChartSpecification(
            id = "slope_chart",
            canonicalName = "Slope chart",
            aliases = listOf("slopegraph", "slope graph", "before after slope", "two point slope"),
            family = ChartFamily.COMPARISON,
            primaryTask = AnalyticalTask.COMPARE_GROUPS,
            supportedTasks = setOf(
                AnalyticalTask.COMPARE_GROUPS,
                AnalyticalTask.COMPARE_CATEGORIES,
                AnalyticalTask.SHOW_CHANGE_OVER_TIME
            ),
            requiredRoles = setOf(
                RoleRequirement.CATEGORY_OR_TIME_DOMAIN,
                RoleRequirement.NUMERIC_MEASURE_OR_RATE,
                RoleRequirement.MULTIPLE_NUMERIC_OR_GROUP
            ),
            optionalRoles = setOf(OptionalRoleSupport.GROUP_SERIES),
            minObservations = 2,
            maxRecommendedCategories = 12,
            supportedCharacteristics = listOf("Paired two-condition or two-period comparison per entity"),
            knownLimitations = listOf("Compares only two points; does not imply a continuous multi-point time trend"),
            perceptualAccuracyRating = 84,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.GOOD,
                score = 84,
                notes = "Two vertical poles with direct entity slopes stay crisp on narrow portrait screens."
            ),
            misleadingDesignRisks = listOf("Drawing a slope across missing pairs or implying intermediate trends"),
            howToRead = "Each line connects an entity's value at the first condition/period to its value at the second.",
            plainWhy = "Shows who increased, decreased, or stayed level between two paired measurements."
        ),
        ChartSpecification(
            id = "bump_chart",
            canonicalName = "Bump chart",
            aliases = listOf("rank chart", "rank trajectory", "ranking over time"),
            family = ChartFamily.TRENDS,
            primaryTask = AnalyticalTask.SHOW_RANKING,
            supportedTasks = setOf(
                AnalyticalTask.SHOW_RANKING,
                AnalyticalTask.SHOW_CHANGE_OVER_TIME,
                AnalyticalTask.COMPARE_GROUPS
            ),
            requiredRoles = setOf(
                RoleRequirement.TIME_DOMAIN,
                RoleRequirement.NUMERIC_MEASURE_OR_RATE,
                RoleRequirement.MULTIPLE_NUMERIC_OR_GROUP
            ),
            optionalRoles = setOf(OptionalRoleSupport.GROUP_SERIES),
            minObservations = 2,
            maxRecommendedCategories = 15,
            supportedCharacteristics = listOf("Ordered rank changes across periods (1 = highest)"),
            knownLimitations = listOf("Rank scale hides absolute magnitude differences; ties share rank via 1224 competition ranking"),
            perceptualAccuracyRating = 85,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.GOOD,
                score = 80,
                notes = "Clear rank lines up to ~6-8 entities across ordered periods."
            ),
            misleadingDesignRisks = listOf("Interpreting rank positions as proportional measurement values"),
            howToRead = "Rank 1 sits at the top; lines trace how each group's rank shifts across periods.",
            plainWhy = "Highlights changes in relative standing without cluttering with raw scale differences."
        ),
        ChartSpecification(
            id = "sparkline_small_multiples",
            canonicalName = "Sparkline small multiples",
            aliases = listOf("sparklines", "small multiples", "mini trends", "sparkline grid"),
            family = ChartFamily.TRENDS,
            primaryTask = AnalyticalTask.SHOW_CHANGE_OVER_TIME,
            supportedTasks = setOf(
                AnalyticalTask.SHOW_CHANGE_OVER_TIME,
                AnalyticalTask.COMPARE_GROUPS
            ),
            requiredRoles = setOf(
                RoleRequirement.TIME_DOMAIN,
                RoleRequirement.NUMERIC_MEASURE_OR_RATE,
                RoleRequirement.MULTIPLE_NUMERIC_OR_GROUP
            ),
            optionalRoles = setOf(OptionalRoleSupport.GROUP_SERIES),
            minObservations = 3,
            supportedCharacteristics = listOf("One compact trend per entity/metric with shared or independent scale"),
            knownLimitations = listOf("Independent scales emphasize shape over magnitude; scale mode is always labelled"),
            perceptualAccuracyRating = 86,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.EXCELLENT,
                score = 90,
                notes = "Stacked compact rows avoid overlapping multi-series lines on portrait phones."
            ),
            misleadingDesignRisks = listOf("Comparing magnitudes across panels when independent Y-scales are active"),
            howToRead = "Each row shows one series' trend over time with its latest value on the right.",
            plainWhy = "Lets you scan many trends cleanly without spaghetti-line overlap."
        ),
        ChartSpecification(
            id = "range_band_chart",
            canonicalName = "Range band chart",
            aliases = listOf("confidence band", "interval band", "fan band", "ribbon chart"),
            family = ChartFamily.STATISTICAL_SPECIALISED,
            primaryTask = AnalyticalTask.SHOW_UNCERTAINTY,
            supportedTasks = setOf(
                AnalyticalTask.SHOW_UNCERTAINTY,
                AnalyticalTask.SHOW_CHANGE_OVER_TIME
            ),
            requiredRoles = setOf(
                RoleRequirement.CATEGORY_OR_TIME_DOMAIN,
                RoleRequirement.ESTIMATE_WITH_LIMITS
            ),
            minObservations = 2,
            supportedCharacteristics = listOf("Central estimate with validated lower <= upper bound ribbon"),
            knownLimitations = listOf("Requires explicit lower and upper bounds; does not invent statistical confidence intervals"),
            perceptualAccuracyRating = 90,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.GOOD,
                score = 82,
                notes = "Shaded band around central line reads cleanly across time or ordered categories."
            ),
            misleadingDesignRisks = listOf("Inverted bounds (lower > upper) or unlabeled intervals mistaken for 95% CIs"),
            howToRead = "The solid line tracks the central value while the shaded band spans lower to upper limits.",
            plainWhy = "Shows both the central trend and the spread or uncertainty around each point."
        ),
        ChartSpecification(
            id = "connected_scatter_plot",
            canonicalName = "Connected scatter plot",
            aliases = listOf("connected scatter", "trajectory scatter", "phase plot"),
            family = ChartFamily.RELATIONSHIP,
            primaryTask = AnalyticalTask.SHOW_RELATIONSHIP,
            supportedTasks = setOf(
                AnalyticalTask.SHOW_RELATIONSHIP,
                AnalyticalTask.SHOW_CHANGE_OVER_TIME
            ),
            requiredRoles = setOf(
                RoleRequirement.TWO_NUMERIC_FIELDS
            ),
            optionalRoles = setOf(OptionalRoleSupport.LABEL),
            minObservations = 3,
            supportedCharacteristics = listOf("Paired (X, Y) values connected strictly in observation sequence"),
            knownLimitations = listOf("Loops or crossings can occur when sequence reverses; never sorts by X"),
            perceptualAccuracyRating = 83,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.MODERATE,
                score = 68,
                notes = "Clear for <= 20 sequential observations with directional arrowheads."
            ),
            misleadingDesignRisks = listOf("Sorting points by X destroys chronological trajectory"),
            howToRead = "Follow the arrows from point to point to see how two metrics move together over sequence.",
            plainWhy = "Reveals joint shifts, cycles, or hysteresis between two numeric variables."
        ),
        ChartSpecification(
            id = "cycle_plot",
            canonicalName = "Cycle plot",
            aliases = listOf("seasonal cycle plot", "month cycle plot", "weekday cycle plot"),
            family = ChartFamily.TRENDS,
            primaryTask = AnalyticalTask.SHOW_CHANGE_OVER_TIME,
            supportedTasks = setOf(
                AnalyticalTask.SHOW_CHANGE_OVER_TIME,
                AnalyticalTask.COMPARE_CATEGORIES
            ),
            requiredRoles = setOf(
                RoleRequirement.TIME_DOMAIN,
                RoleRequirement.NUMERIC_MEASURE_OR_RATE
            ),
            minObservations = 4,
            supportedCharacteristics = listOf("Recurring cycle position sub-trends with per-position mean bars"),
            knownLimitations = listOf("Requires >= 2 full cycles before drawing seasonal conclusions"),
            perceptualAccuracyRating = 87,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.GOOD,
                score = 76,
                notes = "Clear vertical cycle lanes with horizontal mean bars."
            ),
            misleadingDesignRisks = listOf("Inferring seasonality from a single incomplete cycle"),
            howToRead = "Each column groups the same cycle position (e.g. Januaries) across years, with a horizontal mean bar.",
            plainWhy = "Separates recurring seasonal patterns from year-over-year changes within each period."
        ),
        ChartSpecification(
            id = "calendar_heatmap",
            canonicalName = "Calendar heatmap",
            aliases = listOf("calendar grid", "daily heatmap", "date heatmap"),
            family = ChartFamily.TRENDS,
            primaryTask = AnalyticalTask.SHOW_CHANGE_OVER_TIME,
            supportedTasks = setOf(
                AnalyticalTask.SHOW_CHANGE_OVER_TIME,
                AnalyticalTask.SHOW_DISTRIBUTION
            ),
            requiredRoles = setOf(
                RoleRequirement.TIME_DOMAIN,
                RoleRequirement.NUMERIC_MEASURE_OR_RATE
            ),
            minObservations = 3,
            supportedCharacteristics = listOf("Daily ISO dates mapped onto week-by-weekday cells"),
            knownLimitations = listOf("Requires daily dates (yyyy-MM-dd); conflicting same-day values are flagged"),
            perceptualAccuracyRating = 80,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.GOOD,
                score = 78,
                notes = "Compact week-column grid distinguishes missing dates from measured zero."
            ),
            misleadingDesignRisks = listOf("Treating missing dates as zero or silently averaging conflicting same-day rows"),
            howToRead = "Columns are weeks and rows are weekdays; hollow cells are missing dates while tinted cells are measured values.",
            plainWhy = "Spots day-of-week clusters, spikes, and recording gaps across a calendar."
        ),
        ChartSpecification(
            id = "bullet_chart",
            canonicalName = "Bullet chart",
            aliases = listOf("bullet graph", "target bar", "kpi bullet"),
            family = ChartFamily.COMPARISON,
            primaryTask = AnalyticalTask.COMPARE_CATEGORIES,
            supportedTasks = setOf(
                AnalyticalTask.COMPARE_CATEGORIES,
                AnalyticalTask.SHOW_RATES
            ),
            requiredRoles = setOf(
                RoleRequirement.CATEGORY_OR_TIME_DOMAIN,
                RoleRequirement.TWO_NUMERIC_FIELDS
            ),
            minObservations = 1,
            maxRecommendedCategories = 12,
            supportedCharacteristics = listOf("Actual bar from zero against a target marker and optional bands"),
            knownLimitations = listOf("Qualitative bands are user-supplied benchmarks, not clinical reference ranges unless specified"),
            perceptualAccuracyRating = 85,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.GOOD,
                score = 84,
                notes = "Horizontal bullet rows fit portrait screens cleanly with readable labels."
            ),
            misleadingDesignRisks = listOf("Truncating the zero baseline on the actual performance bar"),
            requiresNonNegativeValues = true,
            howToRead = "The solid bar shows the actual value from 0, and the vertical marker shows the target.",
            plainWhy = "Quickly shows whether each ward or metric met, exceeded, or fell short of its target."
        ),
        ChartSpecification(
            id = "dumbbell_chart",
            canonicalName = "Dumbbell chart",
            aliases = listOf("dumbbell plot", "barbell chart", "gap chart", "connected dot comparison"),
            family = ChartFamily.COMPARISON,
            primaryTask = AnalyticalTask.COMPARE_GROUPS,
            supportedTasks = setOf(
                AnalyticalTask.COMPARE_GROUPS,
                AnalyticalTask.COMPARE_CATEGORIES
            ),
            requiredRoles = setOf(
                RoleRequirement.CATEGORY_OR_TIME_DOMAIN,
                RoleRequirement.TWO_NUMERIC_FIELDS
            ),
            minObservations = 2,
            maxRecommendedCategories = 16,
            supportedCharacteristics = listOf("Paired start/end or group A/B values linked by gap stems"),
            knownLimitations = listOf("Requires paired numeric values; missing pairs show only the present point"),
            perceptualAccuracyRating = 85,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.GOOD,
                score = 84,
                notes = "Horizontal paired dots preserve long category names on portrait phones."
            ),
            misleadingDesignRisks = listOf("Misleading scale truncation or connecting a present value to a missing value"),
            howToRead = "Two distinct markers show the two values for each category, with the line showing the gap.",
            plainWhy = "Emphasizes both direction and size of the difference between two paired values."
        ),
        ChartSpecification(
            id = "radar_chart",
            canonicalName = "Radar chart",
            aliases = listOf("spider chart", "web chart", "star plot", "polar chart"),
            family = ChartFamily.COMPARISON,
            primaryTask = AnalyticalTask.COMPARE_GROUPS,
            supportedTasks = setOf(
                AnalyticalTask.COMPARE_GROUPS,
                AnalyticalTask.COMPARE_CATEGORIES
            ),
            requiredRoles = setOf(
                RoleRequirement.CATEGORY_OR_TIME_DOMAIN,
                RoleRequirement.NUMERIC_MEASURE_OR_RATE
            ),
            optionalRoles = setOf(OptionalRoleSupport.GROUP_SERIES),
            minObservations = 3,
            maxRecommendedCategories = 8,
            supportedCharacteristics = listOf("3-8 comparable radial axes sharing a common meaningful scale"),
            knownLimitations = listOf("Only valid for 3-8 axes on a common scale; warns when units or scales differ"),
            perceptualAccuracyRating = 68,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.MODERATE,
                score = 64,
                notes = "Readable up to 6-8 short axis labels; avoid >2 overlapping polygons."
            ),
            misleadingDesignRisks = listOf("Silently normalizing incompatible units or connecting across missing axes"),
            howToRead = "Each spoke represents one comparable metric; outer rings indicate higher values.",
            plainWhy = "Gives a compact multi-metric profile when all axes share the same scale."
        ),

        // --- 8. BATCH 1B STATISTICAL AND DISTRIBUTION CHARTS (GROUP B1) ---
        ChartSpecification(
            id = "diverging_bar_chart",
            canonicalName = "Diverging bar chart",
            aliases = listOf("diverging bar", "diverging horizontal bar", "deviation bar chart", "plus minus bar"),
            family = ChartFamily.COMPARISON,
            primaryTask = AnalyticalTask.COMPARE_CATEGORIES,
            supportedTasks = setOf(
                AnalyticalTask.COMPARE_CATEGORIES,
                AnalyticalTask.COMPARE_GROUPS
            ),
            requiredRoles = setOf(
                RoleRequirement.CATEGORY_DOMAIN,
                RoleRequirement.NUMERIC_MEASURE_OR_RATE
            ),
            minObservations = 2,
            maxRecommendedCategories = 20,
            supportedCharacteristics = listOf("Signed values or deviations extending left and right from a shared zero/midpoint"),
            knownLimitations = listOf("Requires a meaningful zero or midpoint; missing values are omitted"),
            perceptualAccuracyRating = 86,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.EXCELLENT,
                score = 92,
                notes = "Horizontal diverging bars keep category labels readable on portrait phones."
            ),
            misleadingDesignRisks = listOf("Shifting the zero midpoint arbitrarily or coercing missing entries to 0"),
            howToRead = "Bars extend left for negative/below-baseline values and right for positive/above-baseline values.",
            plainWhy = "Highlights direction and magnitude of positive vs negative differences at a glance."
        ),
        ChartSpecification(
            id = "likert_stacked_diverging_chart",
            canonicalName = "Likert stacked diverging chart",
            aliases = listOf("likert chart", "likert diverging", "diverging stacked bar", "survey likert"),
            family = ChartFamily.COMPOSITION,
            primaryTask = AnalyticalTask.SHOW_COMPOSITION,
            supportedTasks = setOf(
                AnalyticalTask.SHOW_COMPOSITION,
                AnalyticalTask.COMPARE_PROPORTIONS,
                AnalyticalTask.COMPARE_GROUPS,
                AnalyticalTask.COMPARE_CATEGORIES
            ),
            requiredRoles = setOf(
                RoleRequirement.CATEGORY_DOMAIN,
                RoleRequirement.NUMERIC_MEASURE_OR_RATE,
                RoleRequirement.MULTIPLE_NUMERIC_OR_GROUP
            ),
            minObservations = 2,
            maxRecommendedCategories = 18,
            supportedCharacteristics = listOf("Ordered survey agreement/disagreement levels centered at 0%"),
            knownLimitations = listOf("Requires at least two ordered response levels"),
            perceptualAccuracyRating = 88,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.GOOD,
                score = 84,
                notes = "Centered 0% axis aligns agree vs disagree totals cleanly on mobile."
            ),
            misleadingDesignRisks = listOf("Reordering Likert levels out of sequence or hiding neutral responses"),
            howToRead = "Disagree responses stack left of 0% and agree responses stack right of 0%; neutral sits across the center.",
            plainWhy = "Lets readers compare net agreement across survey items from a common center line."
        ),
        ChartSpecification(
            id = "waffle_chart",
            canonicalName = "Waffle chart",
            aliases = listOf("waffle", "unit chart", "100 square chart", "grid proportion chart"),
            family = ChartFamily.COMPOSITION,
            primaryTask = AnalyticalTask.SHOW_COMPOSITION,
            supportedTasks = setOf(
                AnalyticalTask.SHOW_COMPOSITION,
                AnalyticalTask.COMPARE_PROPORTIONS,
                AnalyticalTask.COMPARE_CATEGORIES
            ),
            requiredRoles = setOf(
                RoleRequirement.CATEGORY_DOMAIN,
                RoleRequirement.NUMERIC_MEASURE_OR_RATE
            ),
            minObservations = 2,
            maxRecommendedCategories = 8,
            supportedCharacteristics = listOf("Non-negative part-to-whole shares apportioned across a 10x10 (100-cell) unit grid"),
            knownLimitations = listOf("Requires non-negative additive values; small shares <0.5% may round to 0 cells"),
            perceptualAccuracyRating = 84,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.EXCELLENT,
                score = 88,
                notes = "10x10 square grid scales cleanly to any portrait phone width."
            ),
            misleadingDesignRisks = listOf("Using negative values or failing to apportion cells to exactly 100"),
            howToRead = "Each square in the 10x10 grid represents 1% of the total.",
            plainWhy = "Turns percentages into countable unit squares, making proportions intuitive.",
            requiresNonNegativeValues = true
        ),
        ChartSpecification(
            id = "pareto_chart",
            canonicalName = "Pareto chart",
            aliases = listOf("pareto", "pareto diagram", "80 20 chart", "cumulative defect chart"),
            family = ChartFamily.COMPARISON,
            primaryTask = AnalyticalTask.COMPARE_CATEGORIES,
            supportedTasks = setOf(
                AnalyticalTask.COMPARE_CATEGORIES,
                AnalyticalTask.SHOW_COMPOSITION
            ),
            requiredRoles = setOf(
                RoleRequirement.CATEGORY_DOMAIN,
                RoleRequirement.NUMERIC_MEASURE_OR_RATE
            ),
            minObservations = 2,
            maxRecommendedCategories = 20,
            supportedCharacteristics = listOf("Non-negative counts or costs sorted descending with a 0%–100% cumulative curve"),
            knownLimitations = listOf("Requires non-negative additive counts/costs; negative values are rejected"),
            perceptualAccuracyRating = 90,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.GOOD,
                score = 82,
                notes = "Descending bars plus 80% threshold line highlight top contributors quickly."
            ),
            misleadingDesignRisks = listOf("Mixing non-additive rates/averages in the cumulative percentage line"),
            howToRead = "Bars show each category's count from largest to smallest (left axis); the line tracks cumulative % (right axis).",
            plainWhy = "Identifies the vital few categories responsible for most incidents or defects.",
            requiresNonNegativeValues = true
        ),

        // --- 9. BATCH 1B STATISTICAL AND DISTRIBUTION CHARTS (GROUP B2) ---
        ChartSpecification(
            id = "ecdf_chart",
            canonicalName = "Empirical cumulative distribution function (ECDF)",
            aliases = listOf("ecdf", "ecdf plot", "empirical cdf", "cumulative distribution plot"),
            family = ChartFamily.DISTRIBUTION,
            primaryTask = AnalyticalTask.SHOW_DISTRIBUTION,
            supportedTasks = setOf(
                AnalyticalTask.SHOW_DISTRIBUTION,
                AnalyticalTask.COMPARE_GROUPS
            ),
            requiredRoles = setOf(RoleRequirement.NUMERIC_MEASURE_OR_RATE),
            optionalRoles = setOf(OptionalRoleSupport.GROUP_SERIES),
            minObservations = 3,
            supportedCharacteristics = listOf("Stepwise cumulative proportion F_n(x) from 0 to 1 without binning"),
            knownLimitations = listOf("Does not smooth or bin data; missing values are excluded from denominator n"),
            perceptualAccuracyRating = 84,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.GOOD,
                score = 86,
                notes = "Bin-free step curves show exact medians and percentiles cleanly on mobile."
            ),
            misleadingDesignRisks = listOf("Interpolating diagonally between steps instead of drawing right-continuous steps"),
            howToRead = "Move right along values and read up to see the proportion of observations at or below that value.",
            plainWhy = "Shows the exact cumulative distribution without arbitrary histogram bin widths."
        ),
        ChartSpecification(
            id = "qq_plot",
            canonicalName = "Quantile–quantile (Q–Q) plot",
            aliases = listOf("qq plot", "q-q plot", "normal probability plot", "quantile quantile plot"),
            family = ChartFamily.DISTRIBUTION,
            primaryTask = AnalyticalTask.SHOW_DISTRIBUTION,
            supportedTasks = setOf(AnalyticalTask.SHOW_DISTRIBUTION),
            requiredRoles = setOf(RoleRequirement.NUMERIC_MEASURE_OR_RATE),
            minObservations = 3,
            supportedCharacteristics = listOf("Ordered sample quantiles plotted against Normal theoretical quantiles (Blom plotting positions)"),
            knownLimitations = listOf("Quartile reference line requires n >= 4; small samples show wider tail scatter"),
            perceptualAccuracyRating = 84,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.GOOD,
                score = 85,
                notes = "Scatter points against a diagonal quartile line fit cleanly in portrait or square frames."
            ),
            misleadingDesignRisks = listOf("Over-interpreting small-n tail deviations as non-normality"),
            howToRead = "Points lying close to the dashed quartile reference line follow a Normal distribution.",
            plainWhy = "Reveals skewness, heavy tails, and outliers far more clearly than a histogram."
        ),
        ChartSpecification(
            id = "stem_and_leaf_plot",
            canonicalName = "Stem-and-leaf plot",
            aliases = listOf("stem and leaf", "stemplot", "stem-and-leaf", "tukey stemplot"),
            family = ChartFamily.DISTRIBUTION,
            primaryTask = AnalyticalTask.SHOW_DISTRIBUTION,
            supportedTasks = setOf(AnalyticalTask.SHOW_DISTRIBUTION),
            requiredRoles = setOf(RoleRequirement.NUMERIC_MEASURE_OR_RATE),
            minObservations = 3,
            maxRecommendedCategories = 25,
            supportedCharacteristics = listOf("Numeric distribution preserving leading digits (stems) and trailing digits (leaves)"),
            knownLimitations = listOf("Best for moderate sample sizes (n <= ~100) where individual digits remain legible"),
            perceptualAccuracyRating = 87,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.GOOD,
                score = 80,
                notes = "Monospace stem | leaf layout preserves exact values on portrait screens."
            ),
            misleadingDesignRisks = listOf("Omitting the precision key or collapsing -0 and +0 stems"),
            howToRead = "Combine each stem on the left of the line with each leaf digit on the right using the Key.",
            plainWhy = "Shows distribution shape while retaining the actual numerical values."
        ),

        // --- 10. BATCH 1B STATISTICAL AND DISTRIBUTION CHARTS (GROUP B3) ---
        ChartSpecification(
            id = "hexbin_plot",
            canonicalName = "Hexbin plot",
            aliases = listOf("hexbin", "hexagonal binning", "hex density plot", "2d hexbin"),
            family = ChartFamily.DISTRIBUTION,
            primaryTask = AnalyticalTask.SHOW_RELATIONSHIP,
            supportedTasks = setOf(
                AnalyticalTask.SHOW_RELATIONSHIP,
                AnalyticalTask.SHOW_DISTRIBUTION
            ),
            requiredRoles = setOf(RoleRequirement.TWO_NUMERIC_FIELDS),
            minObservations = 10,
            supportedCharacteristics = listOf("2D numeric point density aggregated into regular hexagonal bins"),
            knownLimitations = listOf("Empty cells are omitted to distinguish zero density from zero-valued measurements"),
            perceptualAccuracyRating = 86,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.GOOD,
                score = 83,
                notes = "Hexagonal density shading prevents overplotting on large mobile scatter datasets."
            ),
            misleadingDesignRisks = listOf("Drawing empty bins as zero-valued measurements or using non-uniform bin areas"),
            howToRead = "Darker hexagons contain more paired (X, Y) observations; blank areas have zero observations.",
            plainWhy = "Reveals bivariate density patterns when scatter plots suffer from heavy overplotting."
        ),
        ChartSpecification(
            id = "scatterplot_matrix",
            canonicalName = "Scatterplot matrix",
            aliases = listOf("splom", "pairs plot", "scatter matrix", "pairwise scatter"),
            family = ChartFamily.RELATIONSHIP,
            primaryTask = AnalyticalTask.SHOW_RELATIONSHIP,
            supportedTasks = setOf(AnalyticalTask.SHOW_RELATIONSHIP),
            requiredRoles = setOf(
                RoleRequirement.TWO_NUMERIC_FIELDS,
                RoleRequirement.MULTIPLE_NUMERIC_OR_GROUP
            ),
            minObservations = 5,
            maxRecommendedCategories = 5,
            supportedCharacteristics = listOf("Pairwise scatter subplots across 2–5 numeric variables with labeled diagonals"),
            knownLimitations = listOf("Limited to 5 numeric variables on phone screens to keep subplots legible"),
            perceptualAccuracyRating = 85,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.MODERATE,
                score = 72,
                notes = "Readable up to 4–5 variables; larger matrices become too dense on portrait phones."
            ),
            misleadingDesignRisks = listOf("Overloading with too many variables so individual subplots collapse"),
            howToRead = "Each off-diagonal cell plots the row variable (Y) against the column variable (X).",
            plainWhy = "Screens all pairwise linear, nonlinear, and outlier relationships in one view."
        ),
        ChartSpecification(
            id = "mosaic_plot",
            canonicalName = "Mosaic plot",
            aliases = listOf("marimekko", "marimekko chart", "contingency mosaic", "eikosogram"),
            family = ChartFamily.COMPOSITION,
            primaryTask = AnalyticalTask.SHOW_COMPOSITION,
            supportedTasks = setOf(
                AnalyticalTask.SHOW_COMPOSITION,
                AnalyticalTask.COMPARE_PROPORTIONS,
                AnalyticalTask.COMPARE_GROUPS,
                AnalyticalTask.SHOW_RELATIONSHIP
            ),
            requiredRoles = setOf(
                RoleRequirement.CATEGORY_DOMAIN,
                RoleRequirement.MULTIPLE_NUMERIC_OR_GROUP
            ),
            optionalRoles = setOf(OptionalRoleSupport.GROUP_SERIES),
            minObservations = 4,
            maxRecommendedCategories = 12,
            supportedCharacteristics = listOf("Two-way contingency proportions with column widths = marginal share and tile heights = conditional share"),
            knownLimitations = listOf("Requires non-negative counts/weights across two categorical dimensions"),
            perceptualAccuracyRating = 83,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.GOOD,
                score = 79,
                notes = "Works best with 2–5 columns and 2–5 row categories on mobile."
            ),
            misleadingDesignRisks = listOf("Using equal-width columns when marginal totals differ substantially"),
            howToRead = "Column width shows each group's share of the total; tile height shows the breakdown within that group.",
            plainWhy = "Visualizes both marginal and conditional distributions of a contingency table simultaneously.",
            requiresNonNegativeValues = true
        ),
        ChartSpecification(
            id = "sunburst_chart",
            canonicalName = "Sunburst chart",
            aliases = listOf("sunburst", "multi-level pie", "radial hierarchy", "ring tree"),
            family = ChartFamily.COMPOSITION,
            primaryTask = AnalyticalTask.SHOW_COMPOSITION,
            supportedTasks = setOf(
                AnalyticalTask.SHOW_COMPOSITION,
                AnalyticalTask.COMPARE_PROPORTIONS,
                AnalyticalTask.COMPARE_CATEGORIES
            ),
            requiredRoles = setOf(
                RoleRequirement.CATEGORY_DOMAIN,
                RoleRequirement.NUMERIC_MEASURE_OR_RATE
            ),
            optionalRoles = setOf(OptionalRoleSupport.GROUP_SERIES),
            minObservations = 2,
            maxRecommendedCategories = 16,
            supportedCharacteristics = listOf("Concentric hierarchical rings encoding non-negative part-to-whole proportions"),
            knownLimitations = listOf("Requires non-negative values; deep hierarchies (>3 levels) become hard to read on phones"),
            perceptualAccuracyRating = 74,
            phoneReadability = PhoneReadabilityProfile(
                tier = PhoneReadabilityTier.MODERATE,
                score = 73,
                notes = "1–2 ring levels read cleanly in a square mobile viewport."
            ),
            misleadingDesignRisks = listOf("Encoding negative values or non-additive rates as angular sweeps"),
            howToRead = "Inner ring shows top-level categories; outer rings subdivide each parent slice proportionally.",
            plainWhy = "Shows hierarchical part-to-whole structure and parent-child breakdowns in one radial view.",
            requiresNonNegativeValues = true
        )
    ).map { spec -> enrichBatchSpecification(spec) }

    private fun enrichBatchSpecification(spec: ChartSpecification): ChartSpecification = when (spec.id) {
        "horizontal_bar_chart" -> spec.copy(
            perceptualAccuracyRating = 89,
            phoneReadability = spec.phoneReadability.copy(score = 84),
            howToRead = "Each horizontal bar extends from 0 to show the count or rate for a category.",
            plainWhy = "Ideal when ward or diagnosis names are long or when comparing ranked categories on a phone screen.",
        )
        "lollipop_chart" -> spec.copy(
            perceptualAccuracyRating = 88,
            phoneReadability = spec.phoneReadability.copy(score = 82),
            howToRead = "Follow the horizontal stem from 0 to the circle marker to read each category's value.",
            plainWhy = "Provides the same zero-anchored comparison as a bar chart with lighter visual weight.",
        )
        "dot_plot" -> spec.copy(
            perceptualAccuracyRating = 88,
            phoneReadability = spec.phoneReadability.copy(score = 78),
            howToRead = "Each dot marks the exact value for its category along the horizontal numeric axis.",
            plainWhy = "Allows high-precision comparison across categories or groups without heavy bar fills.",
        )
        "area_chart" -> spec.copy(
            howToRead = "The line traces change over time while the shaded area to zero emphasizes cumulative magnitude.",
            plainWhy = "Highlights both the temporal trend and the non-negative volume beneath the curve.",
        )
        "stacked_area_chart" -> spec.copy(
            howToRead = "Stacked bands show how each non-negative group contributes to the changing total over time.",
            plainWhy = "Shows how both the overall volume and component shares evolve across ordered periods.",
        )
        "step_chart" -> spec.copy(
            perceptualAccuracyRating = 86,
            phoneReadability = spec.phoneReadability.copy(score = 84),
            howToRead = "Horizontal segments hold the current level until a discrete jump occurs at the next observation.",
            plainWhy = "Best when values remain constant between recorded updates (e.g., bed capacity, protocol thresholds).",
        )
        "histogram" -> spec.copy(
            howToRead = "Each bar shows how many observations fall inside that equal-width numeric interval.",
            plainWhy = "Reveals the shape, center, spread, and skewness of a continuous numeric distribution.",
        )
        "box_plot" -> spec.copy(
            howToRead = "The box spans Q1 to Q3 with a median line inside; whiskers extend up to 1.5×IQR and points beyond are outliers.",
            plainWhy = "Summarizes median, interquartile spread, and extreme values across one or more clinical groups.",
        )
        "strip_plot" -> spec.copy(
            howToRead = "Each dot is one individual observation, lightly jittered horizontally so identical values remain visible.",
            plainWhy = "Shows every raw data point directly when sample sizes are small or moderate.",
        )
        "error_bar_chart" -> spec.copy(
            howToRead = "Markers show the central estimate and vertical whiskers span the lower to upper confidence or uncertainty bounds.",
            plainWhy = "Communicates both the point estimate and the precision of that estimate across categories.",
        )
        "range_plot" -> spec.copy(
            howToRead = "Each horizontal bar spans from the lower bound to the upper bound with a marker at the central value.",
            plainWhy = "Compares min–max or confidence interval spans cleanly across categories.",
        )
        "forest_plot" -> spec.copy(
            howToRead = "Squares show each study or subgroup effect estimate with horizontal confidence intervals against the dashed reference line.",
            plainWhy = "Standard clinical view for comparing odds ratios, risk ratios, or treatment effects and their 95% confidence intervals.",
        )
        "control_chart" -> spec.copy(
            howToRead = "The solid center line is the process mean (CL) and dashed lines mark ±3σ limits (LCL/UCL); circled diamonds flag special-cause points.",
            plainWhy = "Distinguishes routine common-cause variation from statistically significant special-cause shifts over time.",
        )
        "kaplan_meier_curve" -> spec.copy(
            howToRead = "The step curve starts at 100% survival at t=0 and drops at event times; '+' ticks mark right-censored observations.",
            plainWhy = "Estimates time-to-event survival probabilities accurately when some patients are censored before experiencing the event.",
        )
        "pie_chart" -> spec.copy(
            howToRead = "Each slice shows a category's percentage share of a single non-negative whole.",
            plainWhy = "Best for showing 2–5 simple part-to-whole proportions at a single point in time.",
        )
        "donut_chart" -> spec.copy(
            howToRead = "Ring sectors show each category's share while the center displays the combined total.",
            plainWhy = "Highlights part-to-whole composition alongside the overall total in a compact ring.",
        )
        "bubble_chart" -> spec.copy(
            howToRead = "Position shows X and Y values while each bubble's area scales proportionally with the third numeric variable.",
            plainWhy = "Reveals relationships across three numeric variables simultaneously.",
        )
        "correlation_heatmap" -> spec.copy(
            howToRead = "Each cell shows the pairwise Pearson correlation r (-1.0 to +1.0) between two numeric variables.",
            plainWhy = "Quickly screens positive and negative linear associations across multiple numeric measures.",
        )
        "waterfall_chart" -> spec.copy(
            howToRead = "Floating bars show sequential positive or negative changes leading up to the final cumulative total.",
            plainWhy = "Explains how an initial balance or count reaches a final total through step-by-step additions and subtractions.",
        )
        "population_pyramid" -> spec.copy(
            howToRead = "Bars extend left and right from the center 0 line to compare two opposing groups across ordered strata.",
            plainWhy = "Ideal for comparing age-by-sex distributions or two opposing demographic cohorts side by side.",
        )
        "violin_plot" -> spec.copy(
            howToRead = "Mirrored density curves show where values cluster alongside a central median and interquartile box.",
            plainWhy = "Reveals multimodal shapes and distribution spread across groups when sample sizes are sufficient (n >= 10).",
        )
        "density_plot" -> spec.copy(
            howToRead = "The smooth curve shows estimated probability density across the numeric range rather than discrete bar counts.",
            plainWhy = "Highlights continuous distribution shape, peaks, and group overlap without histogram bin-edge artifacts.",
        )
        "treemap" -> spec.copy(
            howToRead = "Nested rectangles encode each non-negative category's share of the whole by proportional area.",
            plainWhy = "Displays hierarchical or many-category part-to-whole composition compactly in one view.",
        )
        "funnel_chart" -> spec.copy(
            howToRead = "Centered bars trace sequential stages from top to bottom, showing retention when stages narrow monotonically.",
            plainWhy = "Clearly visualizes ordered screening, triage, or care-cascade stages.",
        )
        else -> spec
    }

    private val byId: Map<String, ChartSpecification> = allSpecifications.associateBy { it.id }

    fun findById(id: String): ChartSpecification? = byId[id]
}
