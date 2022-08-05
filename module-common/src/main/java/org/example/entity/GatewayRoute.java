package org.example.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@Document("gatewayRoute")
public class GatewayRoute {
    @Id
    private ObjectId _id;

    @Field("routeId")
    private String routeId;

    @Field("predicates")
    private List<PredicateDefinition> predicates;

    @Field("filters")
    private List<FilterDefinition> filters;

    @Field("uri")
    private String uri;

    @Field("order")
    private int order;

    @Field("metadata")
    private Map metadata;

    @Field("desc")
    private String desc;

    @Field("enableYn")
    private Boolean enableYn;
}
