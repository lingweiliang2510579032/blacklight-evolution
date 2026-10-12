package com.codex.blacklight.client;

/** Prediction survives network latency; only the matching acknowledgement can settle a request. */
public final class TargetLockRequestState {
    private int requestId,desiredId=-1,acknowledgedId=-1;
    private boolean pending,attachmentObserved;
    public int requestId(){return requestId;}
    public int desiredId(){return desiredId;}
    public int acknowledgedId(){return acknowledgedId;}
    public boolean pending(){return pending;}
    public int request(int id) {
        if(id< -1)throw new IllegalArgumentException("Invalid target ID");
        requestId=requestId==Integer.MAX_VALUE?1:requestId+1;
        desiredId=id;pending=true;attachmentObserved=false;return requestId;
    }
    public boolean acknowledge(int sequence,int acceptedId) {
        if(!pending||sequence!=requestId||acceptedId< -1)return false;
        acknowledgedId=desiredId=acceptedId;pending=false;attachmentObserved=false;return true;
    }
    /** Synced attachments still deliver later server cleanup to the owner and observers. */
    public boolean observeAttachment(int id) {
        if(pending)return false;
        if(id==desiredId){attachmentObserved=true;return false;}
        if(!attachmentObserved)return false;
        desiredId=acknowledgedId=-1;attachmentObserved=false;return true;
    }
    public void resetOwner() {
        // Keep sequence monotonic across respawn/world changes, invalidating old replies.
        requestId=requestId==Integer.MAX_VALUE?1:requestId+1;
        desiredId=acknowledgedId=-1;pending=false;attachmentObserved=false;
    }
}
