package org.starloco.locos.entity.map

class InteractiveObjectTemplate(val id: Int, skills: Collection<Int>, val isWalkable: Boolean) {
    private val skills = HashSet<Int>()

    init {
        this.skills.addAll(skills)
    }

    fun allowSkill(sk: Int): Boolean {
        return skills.contains(sk)
    }
}
