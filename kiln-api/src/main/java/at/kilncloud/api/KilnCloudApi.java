package at.kilncloud.api;

/**
 * Marks the stable integration surface exposed to third-party KilnCloud extensions.
 *
 * <p>Feature-specific contracts will extend this interface as their milestones are implemented. The
 * empty root keeps consumers independent from implementation modules while the foundation is built.
 */
public interface KilnCloudApi {}
