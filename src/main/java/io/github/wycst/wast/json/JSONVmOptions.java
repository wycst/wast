package io.github.wycst.wast.json;

/**
 * provide configuration (control) related to initializing and loading VMs
 * <p>
 * If you want to manually configure it to take effect, you need to set it before initializing the JSON class（JSON第一次调用之前）
 */
public final class JSONVmOptions {

    // vm 关闭intrinsic-candidate优化
    public static final String VM_OPTION_INTRINSIC_CANDIDATE_DISABLED_KEY = "wast.json.intrinsic-candidate.disabled";
    // vm 关闭向量优化
    public static final String VM_OPTION_INCUBATOR_VECTOR_DISABLED_KEY = "wast.json.incubator.vector.disabled";
    // 内存对齐
    public static final String VM_OPTION_REQUIRED_MEMORY_ALIGNMENT_KEY = "wast.json.required-memory-alignment";

    // disabled intrinsic-candidate
    static boolean intrinsicCandidateDisabled;
    // disabled incubator.vector api
    static boolean incubatorVectorDisabled;
    static boolean requiredMemoryAlignment;

    // force disabled
    public static void disableIntrinsicCandidate() {
        intrinsicCandidateDisabled = true;
    }

    // force disabled
    public static void disableIncubatorVector() {
        incubatorVectorDisabled = true;
    }

    // 设置需要内存对齐（JSON第一次调用之前）
    public static void forceRequiredMemoryAlignment() {
        requiredMemoryAlignment = true;
    }

    public static boolean isIntrinsicCandidateDisabled() {
        return intrinsicCandidateDisabled || "true".equalsIgnoreCase(System.getProperty(VM_OPTION_INTRINSIC_CANDIDATE_DISABLED_KEY));
    }

    public static boolean isIncubatorVectorDisabled() {
        return incubatorVectorDisabled || "true".equalsIgnoreCase(System.getProperty(VM_OPTION_INCUBATOR_VECTOR_DISABLED_KEY));
    }

    /**
     * 是否需要内存对齐
     *
     * @return
     */
    public static boolean isRequiredMemoryAlignment() {
        return requiredMemoryAlignment || "true".equalsIgnoreCase(System.getProperty(VM_OPTION_REQUIRED_MEMORY_ALIGNMENT_KEY));
    }
}
