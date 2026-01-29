package cn.szkug.akit.graph.renderscript

data class BlurConfig(
    val radius: Int,
    val repeat: Int = radius,
) {
    companion object {
        private const val MAX_MOD = 25

        fun coerceInMod(mod: Int) = mod.coerceIn(0, MAX_MOD)
    }
}