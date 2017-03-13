package com.teamtreehouse.web.controller;

import com.teamtreehouse.domain.Favorite;
import com.teamtreehouse.service.FavoriteNotFoundException;
import com.teamtreehouse.service.FavoriteService;
import com.teamtreehouse.service.WeatherService;
import com.teamtreehouse.service.dto.geocoding.Geometry;
import com.teamtreehouse.service.dto.geocoding.Location;
import com.teamtreehouse.service.dto.geocoding.PlacesResult;
import com.teamtreehouse.service.dto.weather.Weather;
import com.teamtreehouse.service.resttemplate.PlacesService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;

import static com.teamtreehouse.domain.Favorite.FavoriteBuilder;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(MockitoJUnitRunner.class)
public class FavoriteControllerTest {
    private MockMvc mockMvc;

    @InjectMocks
    private FavoriteController controller;

    @Mock
    private FavoriteService favoriteService;

    @Mock
    private WeatherService weatherService;

    @Mock
    private PlacesService placesService;

    @Before
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void index_ShouldIncludeFavoritesInModel() throws Exception {
        // Arrange the mock behavior
        List<Favorite> favorites = Arrays.asList(
                new FavoriteBuilder(1L).withAddress("Chicago").withPlaceId("chicago1").build(),
                new FavoriteBuilder(2L).withAddress("Omaha").withPlaceId("omaha1").build()
        );
        when(favoriteService.findAll()).thenReturn(favorites);

        // Act (perform the MVC request) and Assert results
        mockMvc.perform(get("/favorites"))
                .andExpect(status().isOk())
                .andExpect(view().name("favorite/index"))
                .andExpect(model().attribute("favorites", favorites));
        verify(favoriteService).findAll();
    }

    @Test
    public void add_ShouldRedirectToNewFavorite() throws Exception {
        // Arrange the mock behavior
        doAnswer(invocation -> {
            Favorite f = (Favorite) invocation.getArguments()[0];
            f.setId(1L);
            return null;
        }).when(favoriteService).save(any(Favorite.class));

        // Act (perform the MVC request) and Assert results
        mockMvc.perform(
                post("/favorites")
                        .param("formattedAddres", "chicago, il")
                        .param("placeId", "windycity")
        ).andExpect(redirectedUrl("/favorites/1"));
        verify(favoriteService).save(any(Favorite.class));
    }

    @Test
    public void detail_ShouldErrorOnNotFound() throws Exception {
        // Arrange the mock behavior
        when(favoriteService.findById(1L)).thenThrow(FavoriteNotFoundException.class);

        // Act (perform the MVC request) and Assert results
        mockMvc.perform(get("/favorites/1"))
                .andExpect(view().name("error"))
                .andExpect(model().attribute("ex", Matchers.instanceOf(FavoriteNotFoundException.class)));
        verify(favoriteService).findById(1L);
    }

    @Test
    public void detail_ShouldRedirectToDetailOnSuccess() throws Exception {
        // Arrange the mock behavior
        Favorite favorite = new FavoriteBuilder(1L).withAddress("Austin").withPlaceId("austinTX").build();
        PlacesResult result = new PlacesResult();
        Weather weather = new Weather();
        Geometry geometry = new Geometry();
        geometry.setLocation(new Location(1L, 2L));
        result.setGeometry(geometry);

        when(favoriteService.findById(1L)).thenReturn(favorite);
        when(placesService.findByPlaceId(favorite.getPlaceId())).thenReturn(result);
        when(weatherService.findByLocation(result.getGeometry().getLocation())).thenReturn(weather);

        // Act (perform the MVC request) and Assert results
        mockMvc.perform(get("/favorites/1"))
                .andExpect(view().name("favorite/detail"))
                .andExpect(model().attribute("favorite", favorite))
                .andExpect(model().attribute("weather", weather));
        verify(favoriteService).findById(1L);
        verify(placesService).findByPlaceId(favorite.getPlaceId());
        verify(weatherService).findByLocation(result.getGeometry().getLocation());
    }
}