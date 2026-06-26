package top.xiaocaohub.aichat.dto;

import lombok.Data;

@Data
public class PageRequest {
    private int page = 0;
    private int size = 20;

    public int getOffset() {
        return page * size;
    }

    public void validate() {
        if (page < 0) page = 0;
        if (size < 1) size = 20;
        if (size > 100) size = 100;
    }
}
