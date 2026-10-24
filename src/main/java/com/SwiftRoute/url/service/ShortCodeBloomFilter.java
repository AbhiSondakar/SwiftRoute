// Deprecated: Removed Bloom Filter to support multi-instance horizontal scaling.
// Collision prevention now handled natively via PostgreSQL unique constraints
// and Redis setIfAbsent (SETNX).
