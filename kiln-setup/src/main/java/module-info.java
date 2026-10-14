/**
 * Defines KilnCloud's setup logic: certificate authority, mTLS identities and join tokens.
 */
module at.kilncloud.setup {
    requires transitive at.kilncloud.common;
    requires org.bouncycastle.pkix;
    requires org.bouncycastle.provider;
    requires static org.jspecify;

    exports at.kilncloud.setup.pki;
    exports at.kilncloud.setup.token;
}
