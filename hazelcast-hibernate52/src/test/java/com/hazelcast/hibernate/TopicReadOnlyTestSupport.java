/*
 * Copyright 2020 Hazelcast Inc.
 *
 * Licensed under the Hazelcast Community License (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://hazelcast.com/hazelcast-community-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.hazelcast.hibernate;

import com.hazelcast.hibernate.entity.AnnotatedEntity;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cache.spi.access.AccessType;
import org.junit.Test;

public abstract class TopicReadOnlyTestSupport extends HibernateTopicTestSupport {

    @Test
    public void testUpdateOneEntityByNaturalId() {
        insertAnnotatedEntities(2);

        Session session = sf.openSession();
        Transaction tx = session.beginTransaction();

        AnnotatedEntity toBeUpdated = session.byNaturalId(AnnotatedEntity.class).using("title", "dummy:1").getReference();
        toBeUpdated.setTitle("dummy101");
        tx.commit();
        session.close();

        assertTopicNotifications(2, CACHE_ANNOTATED_ENTITY + "##NaturalId");
        assertTopicNotifications(4, getTimestampsRegionName());
    }

    @Test
    public void testUpdateEntitiesByNaturalId() {
        insertAnnotatedEntities(2);

        Session session = sf.openSession();
        Transaction tx = session.beginTransaction();

        AnnotatedEntity toBeUpdated = session.byNaturalId(AnnotatedEntity.class).using("title", "dummy:1").getReference();
        toBeUpdated.setTitle("dummy101");

        AnnotatedEntity toBeUpdated2 = session.byNaturalId(AnnotatedEntity.class).using("title", "dummy:0").getReference();
        toBeUpdated2.setTitle("dummy100");
        tx.commit();
        session.close();

        // 5 notifications = 1 eviction, plus for each update: one unlockItem and one afterUpdate
        assertTopicNotifications(5, CACHE_ANNOTATED_ENTITY + "##NaturalId");
        assertTopicNotifications(4, getTimestampsRegionName());
    }

    @Test
    public void testDeleteOneEntityByNaturalId() {
        insertAnnotatedEntities(2);

        Session session = sf.openSession();
        Transaction tx = session.beginTransaction();

        AnnotatedEntity toBeDeleted = session.byNaturalId(AnnotatedEntity.class).using("title", "dummy:1").getReference();
        session.delete(toBeDeleted);
        tx.commit();
        session.close();

        assertTopicNotifications(2, CACHE_ANNOTATED_ENTITY + "##NaturalId");
        assertTopicNotifications(4, getTimestampsRegionName());
    }

    @Test
    public void testDeleteEntitiesByNaturalId() {
        insertAnnotatedEntities(2);

        Session session = sf.openSession();
        Transaction tx = session.beginTransaction();

        AnnotatedEntity toBeDeleted = session.byNaturalId(AnnotatedEntity.class).using("title", "dummy:1").getReference();
        session.delete(toBeDeleted);

        AnnotatedEntity toBeDeleted2 = session.byNaturalId(AnnotatedEntity.class).using("title", "dummy:0").getReference();
        session.delete(toBeDeleted2);
        tx.commit();
        session.close();

        assertTopicNotifications(4, CACHE_ANNOTATED_ENTITY + "##NaturalId");
        assertTopicNotifications(4, getTimestampsRegionName());
    }

    @Override
    protected AccessType getCacheStrategy() {
        return AccessType.READ_ONLY;
    }
}
