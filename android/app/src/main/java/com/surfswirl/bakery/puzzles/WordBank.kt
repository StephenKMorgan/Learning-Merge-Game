package com.surfswirl.bakery.puzzles

/** Everyday words (plenty of cozy and bakery ones) for the word puzzles. Uppercase, by length. */
object WordBank {
    private fun w(s: String) = s.trim().split(Regex("\\s+")).map { it.uppercase() }.distinct()

    val byLength: Map<Int, List<String>> = mapOf(
        4 to w("""
            bake cake loaf roll bean milk tart pies jams oven warm cozy dogs fern rose leaf mint lime pear plum
            corn rice salt soup stew tea' wave sand surf sail boat moon star rain snow wind fire lamp book desk
            door gate home nest bird fish frog duck lion bear wolf deer goat lamb barn farm seed tree bark wood
            gold ruby coin gift sing song play game kite ball bell ring hand foot knee nose face hair blue pink
            gray mild kind calm busy glad hope love wish dish fork cups mugs plan time year week hour city town
            road park pond lake hill rock sock shoe coat hats yarn silk wool soap bath nap' cook heat chip
        """).filter { it.length == 4 && it.all { c -> c.isLetter() } },
        5 to w("""
            bread toast honey sugar flour crust icing cream sweet spice apple peach mango lemon melon grape berry
            olive basil thyme onion salad pasta pizza bagel scone donut fudge candy cocoa latte mocha juice water
            table chair couch shelf plate spoon knife glass towel apron oven' stove timer bowls whisk pecan nutty
            puppy paws treat fetch happy smile laugh dream sleep quiet peace light shine sunny beach ocean river
            field grass bloom tulip daisy lilac maple cedar plant seeds trees birds horse sheep goose zebra tiger
            house porch patio front cabin tower bench fence brick stone heart charm jolly merry party dance music
            piano drums story novel paint brush color green black white brown amber ivory coral bring think write
        """).filter { it.length == 5 && it.all { c -> c.isLetter() } },
        6 to w("""
            butter cookie muffin pastry waffle cheese yogurt cereal pepper ginger nutmeg cherry banana orange tomato
            potato carrot garden flower violet orchid meadow forest valley island harbor sunset breeze summer winter
            spring autumn kitchen' basket teapot coffee cookie mitten candle pillow blanket window carpet mirror
            puppy' kitten rabbit turtle parrot donkey monkey spider beetle golden silver purple yellow bright gentle
            cuddle snuggle giggle smiles family friend sister mother father cousin school pencil crayon marble castle
            dragon wizard knight planet rocket travel ticket market bakery recipe dinner supper picnic sweets cinema
        """).filter { it.length == 6 && it.all { c -> c.isLetter() } },
        7 to w("""
            cupcake pancake biscuit brownie custard caramel vanilla pretzel bagels' teacups sundaes oatmeal granola
            cinnamon' sweeten baking' kitchen blanket morning evening sunrise rainbow weather thunder holiday picnics
            journey village cottage balcony fireside lantern teddies puppies kittens chicken dolphin penguin giraffe
            octopus flowers blossom orchard harvest pumpkin apricot avocado coconut lettuce spinach mustard pickles
            friends cuddles laughter whistle festive diamond crystal emerald treasure' library chapter reading writing
            painter dancing singing musical concert parades fortune welcome comfort freedom kindness balloon cookies
        """).filter { it.length == 7 && it.all { c -> c.isLetter() } },
        8 to w("""
            cinnamon doughnut croissant' shortcake pastries pancakes biscuits brownies cupcakes desserts sandwich
            lemonade smoothie milkshake' meatball dumpling noodles' broccoli zucchini cucumber eggplant mushroom
            blueberry' raspberry' cherries apricots peaches' pumpkins coconuts sunshine snowfall rainbows' moonlight
            starlight daydream sleepily blankets pillows' slippers sweaters cardigan kittens' puppydog doghouse
            treasure fireside chimney birthday holidays vacation seashell surfboard sailboat lighthouse' harbours
            gardener painting sketches stickers postcard notebook calendar library' bookcase cupboard teaspoon
            happiest grateful cheerful friendly peaceful adorable precious sparkles twinkles giggling laughter'
        """).filter { it.length == 8 && it.all { c -> c.isLetter() } },
    )

    val all: Set<String> = byLength.values.flatten().toSet()

    fun words(length: Int) = byLength[length.coerceIn(4, 8)]!!

    /** Every word in the bank that uses exactly these letters (for accepting other anagrams). */
    fun anagrams(word: String): List<String> {
        val key = word.toCharArray().sorted()
        return all.filter { it.length == word.length && it.toCharArray().sorted() == key }
    }
}
