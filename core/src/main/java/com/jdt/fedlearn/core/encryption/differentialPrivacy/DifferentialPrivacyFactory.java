package com.jdt.fedlearn.core.encryption.differentialPrivacy;

import com.jdt.fedlearn.core.exception.NotImplementedException;
import com.jdt.fedlearn.core.type.DifferentialPrivacyType;

public class DifferentialPrivacyFactory {

    public static IDifferentialPrivacy createDifferentialPrivacy(DifferentialPrivacyType type){
        IDifferentialPrivacy dp = null;
        switch (type) {
            case OUTPUT_PERTURB:
                dp = new OutputPerturbDPImpl();
                break;
            case OBJECTIVE_PERTURB:
                dp = new ObjectivePerturbDPImpl();
                break;
            default:
                throw new NotImplementedException(type + ": not implemented differential privacy type");
        }
        return dp;
    }

}
