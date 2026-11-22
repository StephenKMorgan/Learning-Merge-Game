package com.surfswirl.bakery.game

/** One line of dialogue: who speaks, and what they say. */
data class Line(val who: String, val text: String)

data class Task(
    val id: String,
    val title: String,
    val art: String,
    val cost: Int,
    /** Where the decoration sits in the scene, as fractions of the scene size. */
    val x: Float,
    val y: Float,
    val size: Float,
    val after: List<Line>,
)

data class Chapter(
    val id: String,
    val title: String,
    val scene: String,
    val intro: List<Line>,
    val tasks: List<Task>,
    val reward: Reward,
)

/**
 * The renovation story. Task ids are stored in the save, so never rename them; add new chapters at
 * the end. A chapter is unlocked when the previous one is finished.
 */
object Story {
    val intro = listOf(
        Line("mandy", "Well, Surf… here it is. Our very own bakery on Maple Lane!"),
        Line("surf", "Woof! *sniff sniff sniff*"),
        Line("mandy", "It's dusty and a little sad, but somehow it already smells like cinnamon rolls in here. Just like home."),
        Line("june", "Welcome, dear! This old bakery has been empty for years. The whole street has missed fresh bread."),
        Line("mandy", "Then let's get baking! Every order we fill earns coins to fix this place up."),
        Line("surf", "*tail wagging intensifies*"),
    )

    val chapters = listOf(
        Chapter("ch1", "The Grand Reopening", "inside", listOf(
            Line("mandy", "First things first: let's make this place cozy again."),
        ), listOf(
            Task("c1_sweep", "Sweep out the dust", "broom", 40, 0.12f, 0.80f, 0.13f, listOf(
                Line("mandy", "Much better! Surf, please stop chasing the broom."),
                Line("surf", "Woof!"))),
            Task("c1_door", "Fix the front door", "door", 60, 0.86f, 0.52f, 0.22f, listOf(
                Line("walt", "That door used to squeak so loud you could hear it from the harbor road!"),
                Line("mandy", "Now it just swings open… and lets the cinnamon smell out onto the street."))),
            Task("c1_windows", "Wash the windows", "window", 80, 0.22f, 0.30f, 0.2f, listOf(
                Line("lily", "Mommy, look! There's a puppy in the bakery window!"),
                Line("surf", "*presses nose against the clean glass*"),
                Line("mandy", "…and there go my clean windows."))),
            Task("c1_picture", "Hang a cozy picture", "framed_picture", 100, 0.52f, 0.22f, 0.15f, listOf(
                Line("ivy", "I painted the view from the hill. Every bakery needs a little art!"),
                Line("mandy", "It's beautiful, Ivy. Thank you!"))),
            Task("c1_chairs", "Set up café chairs", "chair", 140, 0.38f, 0.74f, 0.15f, listOf(
                Line("june", "Oh, a place to sit! I'll be here every morning, you know."),
                Line("mandy", "I was counting on it, June."))),
            Task("c1_bell", "Put out a service bell", "bellhop_bell", 160, 0.62f, 0.56f, 0.1f, listOf(
                Line("max", "*ding ding ding ding ding*"),
                Line("mandy", "Okay Max, one ding is plenty!"))),
            Task("c1_lights", "Hang warm lights", "light_bulb", 200, 0.36f, 0.08f, 0.1f, listOf(
                Line("mandy", "Golden lights, golden crusts. Now it really feels like home."))),
            Task("c1_bed", "Make Surf a comfy bed", "bed", 240, 0.12f, 0.62f, 0.17f, listOf(
                Line("mandy", "Surf, here's your very own bed! Wait… what's under the blanket?"),
                Line("mandy", "Three. Missing. Cinnamon rolls."),
                Line("surf", "*looks at the ceiling innocently*"),
                Line("dot", "Case closed. I always suspect the one with the crumbs on his nose."))),
            Task("c1_clock", "Hang a clock", "mantelpiece_clock", 280, 0.70f, 0.22f, 0.12f, listOf(
                Line("officer", "Is it cinnamon roll o'clock yet?"),
                Line("mandy", "Lou, in this bakery it's ALWAYS cinnamon roll o'clock."))),
            Task("c1_nook", "Build a reading nook", "couch_and_lamp", 350, 0.62f, 0.78f, 0.22f, listOf(
                Line("ms_park", "A quiet corner with a book and a warm roll. I may never leave."),
                Line("surf", "*claims the couch*"))),
            Task("c1_display", "Cinnamon roll display", "roll_tray", 420, 0.50f, 0.50f, 0.17f, listOf(
                Line("chef", "Mamma mia, what a display! The whole street can smell it."),
                Line("mandy", "That's the secret. The smell does all the advertising!"))),
            Task("c1_party", "Grand reopening party!", "party_popper", 500, 0.86f, 0.12f, 0.12f, listOf(
                Line("mandy", "Thank you all for coming! Welcome to Surf & Swirl Bakery!"),
                Line("june", "Best cinnamon rolls on Maple Lane. Maybe the whole world."),
                Line("surf", "WOOF! (Chapter 1 complete!)"))),
        ), Reward(rubies = 25, energy = 100, items = listOf("chest:2"))),

        Chapter("ch2", "Surf's Sunny Patio", "patio", listOf(
            Line("kai", "Hey Mandy! You know what this place needs? A patio. Fresh air, sunshine…"),
            Line("mandy", "…and room for Surf to nap in the sun. Let's do it!"),
        ), listOf(
            Task("c2_weeds", "Pull the weeds", "seedling", 300, 0.15f, 0.82f, 0.12f, listOf(
                Line("bea", "Careful, some of those are herbs! Rosemary goes lovely in bread."))),
            Task("c2_tulips", "Plant tulips", "tulip", 350, 0.30f, 0.82f, 0.12f, listOf(
                Line("bea", "Tulips by the fence. Perfect!"),
                Line("surf", "*sniffs every single tulip*"))),
            Task("c2_umbrella", "Open a patio umbrella", "umbrella_on_ground", 400, 0.50f, 0.42f, 0.24f, listOf(
                Line("kai", "Shade for the customers, sunshine for Surf. Everybody wins."))),
            Task("c2_tables", "Set outdoor tables", "fork_and_knife_with_plate", 450, 0.50f, 0.66f, 0.15f, listOf(
                Line("rosa", "I'm moving my lunch break here. Permanently."))),
            Task("c2_planters", "Add flower planters", "potted_plant", 500, 0.85f, 0.66f, 0.14f, listOf(
                Line("bea", "A garden bakery! My two favorite things."))),
            Task("c2_candles", "Light lanterns", "candle", 550, 0.18f, 0.40f, 0.1f, listOf(
                Line("mandy", "For cozy evenings. With cinnamon rolls, obviously."))),
            Task("c2_balloons", "Balloon arch", "balloon", 600, 0.85f, 0.30f, 0.14f, listOf(
                Line("lily", "Balloons! Is it somebody's birthday?"),
                Line("mandy", "It's the patio's birthday!"))),
            Task("c2_sun", "Surf's sunny spot", "sun", 650, 0.82f, 0.08f, 0.14f, listOf(
                Line("surf", "*flops over in the warm sun, paws in the air*"),
                Line("mandy", "I think he approves."))),
            Task("c2_bouquet", "Bouquet centerpiece", "bouquet", 700, 0.30f, 0.62f, 0.12f, listOf(
                Line("walt", "Flowers on every table. June will love it."))),
            Task("c2_party", "Patio party!", "confetti_ball", 800, 0.62f, 0.14f, 0.13f, listOf(
                Line("kai", "Best patio in town, dude!"),
                Line("mandy", "And it still smells like cinnamon rolls out here. I don't know how, but it does."),
                Line("surf", "WOOF! (Chapter 2 complete! More story coming in a future update.)"))),
        ), Reward(rubies = 40, energy = 150, items = listOf("chest:3"))),
    )

    fun chapterIndex(done: Set<String>): Int {
        for ((i, c) in chapters.withIndex()) if (!c.tasks.all { it.id in done }) return i
        return chapters.size
    }

    fun nextTask(done: Set<String>): Task? = chapters.getOrNull(chapterIndex(done))?.tasks?.firstOrNull { it.id !in done }
    fun allTasks() = chapters.flatMap { it.tasks }
    fun task(id: String) = allTasks().firstOrNull { it.id == id }
    fun chapterOf(taskId: String) = chapters.first { c -> c.tasks.any { it.id == taskId } }
}
