package de.officeryoda.fhysics.engine.collision

object MinimizeOverlap : CollisionSolver() {

    override fun solveCollision(info: CollisionInfo) {
        // that's everything
        separateOverlappingObjects(info)
    }
}
