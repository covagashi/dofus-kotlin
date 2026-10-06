package org.starloco.locos.entity.map

import org.starloco.locos.job.JobConstant

class InteractiveObject(val id: Int) {

    var state: Int = JobConstant.IOBJECT_STATE_FULL

}
