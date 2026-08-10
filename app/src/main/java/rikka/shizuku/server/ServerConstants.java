package rikka.shizuku.server;

public class ServerConstants {

    public static final int MANAGER_APP_NOT_FOUND = 50;

    public static final String PERMISSION = "moe.shizuku.manager.permission.API_V23";

    /** 管理端（FolkPatch）应用包名 */
    public static final String MANAGER_APPLICATION_ID = "me.yuki.folk";
    public static final String REQUEST_PERMISSION_ACTION = MANAGER_APPLICATION_ID + ".intent.action.REQUEST_PERMISSION";

    public static final int BINDER_TRANSACTION_getApplications = 10001;

    /** manager-only：读取某 uid 的分权（shellOnly）标记 */
    public static final int BINDER_TRANSACTION_getShellOnly = 10002;

    /** manager-only：设置某 uid 的分权（shellOnly）标记 */
    public static final int BINDER_TRANSACTION_setShellOnly = 10003;
}
