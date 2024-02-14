package de.officeryoda.fhysics.engine.collision

object MinimizeOverlap : CollisionSolver() {

    override fun solveCollision(points: CollisionInfo) {
        // that's everything
        separateOverlappingObjects(points)
    }
}
