package org.example.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document("accountGrant")
@Getter
@Setter
@Builder
public class AccountGrant {
    @Id
    private ObjectId _id;

    @Indexed
    private ObjectId tntId;

    private String pattern;

    private List<String> method;

    private List<String> grantRole;
}
